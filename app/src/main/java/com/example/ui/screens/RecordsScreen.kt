package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import coil.compose.AsyncImage
import com.example.ui.components.ProofImageViewerDialog
import java.io.File
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.TransactionType
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.EditTransactionSheet
import com.example.ui.components.EmptyListPlaceholder
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardEvent
import com.example.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val baseCurrency = uiState.baseCurrency
    val canUndo by viewModel.canUndo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<String?>("All") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var previewProofTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is DashboardEvent.TransactionLogged -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = if (event.canUndo) "UNDO" else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLastTransaction()
                    }
                }
                is DashboardEvent.TransactionUndone -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is DashboardEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.error,
                        duration = SnackbarDuration.Short
                    )
                }
                else -> {}
            }
        }
    }

    // Filter transactions based on type, account, and search query
    val filteredTransactions = remember(allTransactions, selectedTypeFilter, selectedAccountId, searchQuery) {
        allTransactions.filter { tx ->
            val matchesType = when (selectedTypeFilter) {
                null, "All" -> true
                else -> tx.type.equals(selectedTypeFilter, ignoreCase = true)
            }

            val matchesAccount = when (selectedAccountId) {
                null -> true
                else -> tx.accountId == selectedAccountId || tx.toAccountId == selectedAccountId
            }

            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                        tx.labels.any { it.contains(searchQuery, ignoreCase = true) }
            }

            matchesType && matchesAccount && matchesQuery
        }
    }

    // Group transactions by date day key
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val cal = Calendar.getInstance().apply { time = tx.date }
            "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Master Records Ledger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canUndo) {
                        IconButton(
                            onClick = { viewModel.undoLastTransaction() },
                            modifier = Modifier.testTag("records_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Last Transaction",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by note or #label...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Row (Chips)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type filters
                val typeOptions = listOf("All", "Income", "Expense", "Transfer")
                items(typeOptions) { type ->
                    val isSelected = selectedTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = type },
                        label = { Text(type) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                // Account filters
                items(accounts) { account ->
                    val isSelected = selectedAccountId == account.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedAccountId = if (isSelected) null else account.id
                        },
                        label = { Text(account.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Transaction Summary Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTransactions.size} TRANSACTIONS FOUND",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (selectedAccountId != null || selectedTypeFilter != "All" || searchQuery.isNotEmpty()) {
                    Text(
                        text = "Filtered view",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                val hasActiveFilters = searchQuery.isNotBlank() || selectedTypeFilter != "All" || selectedAccountId != null
                EmptyListPlaceholder(
                    icon = Icons.Default.ReceiptLong,
                    title = "No Transactions Found",
                    message = if (hasActiveFilters) {
                        "Try clearing your search query or active filters to view records."
                    } else {
                        "Your master ledger is empty. Start logging transactions to view historical cash movements."
                    },
                    actionLabel = if (hasActiveFilters) "Clear Filters" else null,
                    onAction = {
                        searchQuery = ""
                        selectedTypeFilter = "All"
                        selectedAccountId = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val accountMap = remember(accounts) { accounts.associateBy { it.id } }
                val categoryMap = remember(categories) { categories.associateBy { it.id } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    groupedTransactions.forEach { (dayKey, txsInDay) ->
                        val headerDate = txsInDay.firstOrNull()?.date ?: Date()
                        val headerText = formatHeaderDate(headerDate)
                        val netDay = txsInDay.sumOf { tx ->
                            when (tx.type) {
                                TransactionType.INCOME.displayName -> tx.amount
                                TransactionType.EXPENSE.displayName -> -tx.amount
                                else -> 0.0
                            }
                        }

                        stickyHeader(key = "header_$dayKey", contentType = "date_header") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = headerText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%s%s",
                                            if (netDay >= 0) "+" else "-",
                                            CurrencyUtils.formatCurrency(kotlin.math.abs(netDay), baseCurrency)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netDay >= 0) Color(0xFF00C853) else Color(0xFFFF3B30)
                                    )
                                }
                            }
                        }

                        items(
                            items = txsInDay,
                            key = { it.id },
                            contentType = { "record_row" }
                        ) { tx ->
                            val sourceAccount = accountMap[tx.accountId]
                            val destAccount = tx.toAccountId?.let { accountMap[it] }
                            val category = tx.categoryId?.let { categoryMap[it] }

                            MasterRecordItemRow(
                                transaction = tx,
                                sourceAccount = sourceAccount,
                                destAccount = destAccount,
                                category = category,
                                currencyCode = sourceAccount?.currencyCode ?: baseCurrency,
                                onEdit = { transactionToEdit = tx },
                                onDelete = { viewModel.deleteTransaction(tx) },
                                onViewProof = { previewProofTransaction = tx },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val currentAccount = accounts.find { it.id == selectedAccountId }
        AddTransactionSheet(
            accounts = accounts,
            categories = categories,
            baseCurrency = currentAccount?.currencyCode ?: baseCurrency,
            initialAccountId = selectedAccountId,
            onDismissRequest = { showAddSheet = false },
            onSaveTransaction = { amount, type, accId, toAccId, catId, labels, note, proofUri ->
                viewModel.addQuickTransaction(
                    amount = amount,
                    type = type,
                    accountId = accId,
                    toAccountId = toAccId,
                    categoryId = catId,
                    labels = labels,
                    note = note,
                    proofImageUri = proofUri
                )
                showAddSheet = false
            }
        )
    }

    // Edit Transaction Sheet
    transactionToEdit?.let { tx ->
        EditTransactionSheet(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            baseCurrency = baseCurrency,
            onDismissRequest = { transactionToEdit = null },
            onSaveTransaction = { updatedTx ->
                viewModel.updateTransaction(updatedTx, tx)
                transactionToEdit = null
            },
            onDeleteTransaction = {
                viewModel.deleteTransaction(tx)
                transactionToEdit = null
            }
        )
    }

    // Proof Photo Full View Dialog
    previewProofTransaction?.let { tx ->
        tx.proofImageUri?.let { uri ->
            val srcAcc = accounts.find { it.id == tx.accountId }
            val cat = categories.find { it.id == tx.categoryId }
            ProofImageViewerDialog(
                imageUri = uri,
                title = tx.note?.takeIf { it.isNotBlank() } ?: cat?.name ?: tx.type,
                amountText = CurrencyUtils.formatCurrency(tx.amount, srcAcc?.currencyCode ?: baseCurrency),
                subtitle = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(tx.date),
                onDismissRequest = { previewProofTransaction = null }
            )
        }
    }
}

@Composable
fun MasterRecordItemRow(
    transaction: TransactionEntity,
    sourceAccount: AccountEntity?,
    destAccount: AccountEntity?,
    category: CategoryEntity?,
    currencyCode: String = "USD",
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onViewProof: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isIncome = remember(transaction.type) { transaction.type.equals(TransactionType.INCOME.displayName, ignoreCase = true) }
    val isExpense = remember(transaction.type) { transaction.type.equals(TransactionType.EXPENSE.displayName, ignoreCase = true) }
    val isTransfer = remember(transaction.type) { transaction.type.equals(TransactionType.TRANSFER.displayName, ignoreCase = true) }

    val amountColor = remember(isIncome, isExpense) {
        when {
            isIncome -> Color(0xFF00C853)
            isExpense -> Color(0xFFFF3B30)
            else -> Color(0xFF1976D2)
        }
    }

    val typeIcon = remember(isIncome, isExpense) {
        when {
            isIncome -> Icons.Default.ArrowDownward
            isExpense -> Icons.Default.ArrowUpward
            else -> Icons.Default.SwapHoriz
        }
    }

    val formattedTime = remember(transaction.date) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(transaction.date)
    }

    val titleText = remember(transaction.note, category?.name, isTransfer, transaction.type) {
        transaction.note?.takeIf { it.isNotBlank() }
            ?: category?.name
            ?: (if (isTransfer) "Transfer" else transaction.type)
    }

    val accountDesc = remember(isTransfer, destAccount?.name, sourceAccount?.name) {
        when {
            isTransfer && destAccount != null -> "${sourceAccount?.name ?: "Account"} → ${destAccount.name}"
            else -> sourceAccount?.name ?: "Main Account"
        }
    }

    val formattedAmount = remember(transaction.amount, currencyCode, isIncome, isExpense) {
        val prefix = if (isIncome) "+" else if (isExpense) "-" else ""
        "$prefix${CurrencyUtils.formatCurrency(transaction.amount, currencyCode)}"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onEdit,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type indicator circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Photo Proof Badge
                    if (!transaction.proofImageUri.isNullOrBlank()) {
                        val file = File(transaction.proofImageUri)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.clickable { onViewProof?.invoke() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Photo Proof",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Proof",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Proof",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = accountDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (transaction.labels.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        transaction.labels.take(3).forEach { label ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "#$label",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Amount & Edit / Delete action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatHeaderDate(date: Date): String {
    val calNow = Calendar.getInstance()
    val calTx = Calendar.getInstance().apply { time = date }

    return if (calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR) &&
        calNow.get(Calendar.DAY_OF_YEAR) == calTx.get(Calendar.DAY_OF_YEAR)
    ) {
        "Today, " + SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    } else {
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        if (calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTx.get(Calendar.DAY_OF_YEAR)
        ) {
            "Yesterday, " + SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(date)
        }
    }
}
