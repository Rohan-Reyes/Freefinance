package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.TransactionType
import com.example.ui.components.AddAccountSheet
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.EmptyListPlaceholder
import com.example.ui.components.ProofImageViewerDialog
import com.example.util.BankAppLauncher
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardEvent
import com.example.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * AccountLogScreen: Dedicated transaction ledger & deep analytics for a specific Account / Sub-Account.
 * Supports viewing history, photo proof previewing, mode analytics (inflow/outflow, category breakdown, photo gallery),
 * and editing account settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountLogScreen(
    accountId: Long,
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val hideMoney by viewModel.hideMoney.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val account = accounts.find { it.id == accountId }
    val parentAccount = remember(account, accounts) {
        account?.parentAccountId?.let { pid -> accounts.find { it.id == pid } }
    }
    val childSubAccounts = remember(account, accounts) {
        account?.id?.let { aid -> accounts.filter { it.parentAccountId == aid } } ?: emptyList()
    }

    val accountTransactionsFlow = remember(accountId) {
        viewModel.getTransactionsForAccount(accountId)
    }
    val transactions by accountTransactionsFlow.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ledger, 1: Account Analytics
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditAccountSheet by remember { mutableStateOf(false) }
    var showAddSubAccountSheet by remember { mutableStateOf(false) }
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var previewProofTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedFilterType by remember { mutableStateOf("All") } // "All", "Expense", "Income", "Transfer"

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

    val matchingBankApp = remember(account) {
        account?.let { BankAppLauncher.findMatchingBankApp(it.name, it.linkedAppPackage) }
    }

    val filteredTransactions = remember(transactions, selectedFilterType) {
        if (selectedFilterType == "All") {
            transactions
        } else {
            transactions.filter { it.type.equals(selectedFilterType, ignoreCase = true) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (parentAccount != null) "${parentAccount.name} › ${account?.name ?: ""}" else (account?.name ?: "Account"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (parentAccount != null) "Sub-Account (${account?.subAccountType ?: account?.type ?: "Sub"})" else (account?.type ?: "Account"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("account_log_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (canUndo) {
                        IconButton(
                            onClick = { viewModel.undoLastTransaction() },
                            modifier = Modifier.testTag("account_log_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Transaction",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (account != null) {
                        IconButton(
                            onClick = {
                                BankAppLauncher.launchBankApp(context, account.name, account.linkedAppPackage)
                            },
                            modifier = Modifier.testTag("account_log_quick_launch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Open ${matchingBankApp?.displayName ?: "Bank App"}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleHideMoney(!hideMoney) },
                            modifier = Modifier.testTag("account_log_hide_money_button")
                        ) {
                            Icon(
                                imageVector = if (hideMoney) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideMoney) "Show Balances" else "Hide Balances",
                                tint = if (hideMoney) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showAdjustBalanceDialog = true },
                            modifier = Modifier.testTag("account_log_adjust_balance_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Calibrate / Adjust Balance",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showEditAccountSheet = true },
                            modifier = Modifier.testTag("account_log_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Account Properties",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("account_log_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Account",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_account_transaction_fab")
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
            // Mode Header Card
            if (account != null) {
                AccountHeaderCard(
                    account = account,
                    parentAccount = parentAccount,
                    childSubAccounts = childSubAccounts,
                    hideMoney = hideMoney,
                    onEditClick = { showEditAccountSheet = true },
                    onAdjustBalanceClick = { showAdjustBalanceDialog = true },
                    onAddSubAccountClick = { showAddSubAccountSheet = true },
                    onToggleTally = { include ->
                        viewModel.toggleAccountTally(account, include)
                    },
                    onOpenBankApp = {
                        BankAppLauncher.launchBankApp(context, account.name, account.linkedAppPackage)
                    }
                )
            }

            // Tab Navigation: Ledger vs Account Analytics
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Ledger (${transactions.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Account Analytics", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: TRANSACTIONS LEDGER
                Column(modifier = Modifier.fillMaxSize()) {
                    // Quick Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Expense", "Income", "Transfer").forEach { filterName ->
                            FilterChip(
                                selected = selectedFilterType == filterName,
                                onClick = { selectedFilterType = filterName },
                                label = { Text(filterName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        EmptyListPlaceholder(
                            icon = Icons.Default.ReceiptLong,
                            title = "No Transactions Found",
                            message = if (selectedFilterType != "All") "No $selectedFilterType transactions logged for this account yet." else "No transactions logged for this account yet. Tap + to add one.",
                            actionLabel = "Log Entry",
                            onAction = { showAddSheet = true },
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        val categoryMap = remember(categories) { categories.associateBy { it.id } }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = filteredTransactions,
                                key = { it.id },
                                contentType = { "account_tx_item" }
                            ) { tx ->
                                val category = tx.categoryId?.let { categoryMap[it] }
                                AccountTransactionItem(
                                    transaction = tx,
                                    category = category,
                                    currencyCode = account?.currencyCode ?: "USD",
                                    hideMoney = hideMoney,
                                    onViewProof = { previewProofTransaction = tx },
                                    onDeleteClick = { viewModel.deleteTransaction(tx) }
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: ACCOUNT SPECIFIC ANALYTICS
                if (account != null) {
                    AccountAnalyticsView(
                        account = account,
                        transactions = transactions,
                        categories = categories,
                        hideMoney = hideMoney,
                        onViewProof = { tx -> previewProofTransaction = tx }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            accounts = accounts,
            categories = categories,
            baseCurrency = account?.currencyCode ?: "USD",
            initialAccountId = accountId,
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

    if (showEditAccountSheet && account != null) {
        AddAccountSheet(
            defaultCurrency = account.currencyCode,
            initialAccount = account,
            allAccounts = accounts,
            onDismissRequest = { showEditAccountSheet = false },
            onSaveAccount = { name, type, balance, color, interestRate, compounding, currencyCode, isLocked, lockedUntil, linkedAppPackage, includeInTally, parentAccountId, subAccountType ->
                val updatedAccount = account.copy(
                    name = name,
                    type = type,
                    currentBalance = balance,
                    color = color,
                    interestRatePa = interestRate,
                    compoundingFrequency = compounding,
                    currencyCode = currencyCode,
                    isLocked = isLocked,
                    lockedUntil = lockedUntil,
                    linkedAppPackage = linkedAppPackage,
                    includeInTally = includeInTally,
                    parentAccountId = parentAccountId,
                    subAccountType = subAccountType
                )
                viewModel.updateAccount(updatedAccount)
                showEditAccountSheet = false
            },
            onDeleteAccount = { accToDelete ->
                viewModel.deleteAccount(accToDelete)
                showEditAccountSheet = false
                onNavigateBack()
            }
        )
    }

    if (showAddSubAccountSheet && account != null) {
        AddAccountSheet(
            defaultCurrency = account.currencyCode,
            allAccounts = accounts,
            preselectedParentAccountId = account.id,
            onDismissRequest = { showAddSubAccountSheet = false },
            onSaveAccount = { name, type, balance, color, interestRate, compounding, currencyCode, isLocked, lockedUntil, linkedAppPackage, includeInTally, parentAccountId, subAccountType ->
                viewModel.addAccount(
                    name = name,
                    type = type,
                    initialBalance = balance,
                    color = color,
                    interestRatePa = interestRate,
                    compoundingFrequency = compounding,
                    currencyCode = currencyCode,
                    isLocked = isLocked,
                    lockedUntil = lockedUntil,
                    linkedAppPackage = linkedAppPackage,
                    includeInTally = includeInTally,
                    parentAccountId = parentAccountId ?: account.id,
                    subAccountType = subAccountType
                )
                showAddSubAccountSheet = false
            }
        )
    }

    if (showAdjustBalanceDialog && account != null) {
        CalibrateBalanceDialog(
            account = account,
            onDismiss = { showAdjustBalanceDialog = false },
            onSave = { newBalance ->
                viewModel.adjustAccountBalance(account.id, newBalance)
                showAdjustBalanceDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (account.parentAccountId != null) "Delete Sub-Account?" else "Delete Account?") },
            text = { Text("Are you sure you want to delete '${account.name}'? Existing transactions will keep their records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(account)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Proof Photo Viewer Modal
    previewProofTransaction?.let { tx ->
        tx.proofImageUri?.let { uri ->
            val cat = categories.find { it.id == tx.categoryId }
            ProofImageViewerDialog(
                imageUri = uri,
                title = tx.note?.takeIf { it.isNotBlank() } ?: cat?.name ?: tx.type,
                amountText = CurrencyUtils.formatCurrency(tx.amount, account?.currencyCode ?: "USD"),
                subtitle = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(tx.date),
                onDismissRequest = { previewProofTransaction = null }
            )
        }
    }
}

/**
 * AccountAnalyticsView: Deep analytics specifically for this account.
 */
@Composable
private fun AccountAnalyticsView(
    account: AccountEntity,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    hideMoney: Boolean,
    onViewProof: (TransactionEntity) -> Unit
) {
    val currencyCode = account.currencyCode
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val totalInflow = remember(transactions) {
        transactions.filter {
            it.type.equals(TransactionType.INCOME.displayName, ignoreCase = true) ||
                    (it.type.equals(TransactionType.TRANSFER.displayName, ignoreCase = true) && it.toAccountId == account.id)
        }.sumOf { it.amount }
    }

    val totalOutflow = remember(transactions) {
        transactions.filter {
            it.type.equals(TransactionType.EXPENSE.displayName, ignoreCase = true) ||
                    (it.type.equals(TransactionType.TRANSFER.displayName, ignoreCase = true) && it.accountId == account.id)
        }.sumOf { it.amount }
    }

    val netCashflow = totalInflow - totalOutflow
    val totalExpenseTransactions = remember(transactions) {
        transactions.filter { it.type.equals(TransactionType.EXPENSE.displayName, ignoreCase = true) }
    }
    val avgExpense = if (totalExpenseTransactions.isNotEmpty()) totalOutflow / totalExpenseTransactions.size else 0.0

    // Group expenses by category
    val categorySpending = remember(transactions, categoryMap) {
        totalExpenseTransactions
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val catName = catId?.let { categoryMap[it]?.name } ?: "Uncategorized"
                val catColor = catId?.let { categoryMap[it]?.color } ?: 0xFF888888.toInt()
                val sum = txs.sumOf { it.amount }
                val count = txs.size
                Triple(catName, sum, count) to catColor
            }
            .sortedByDescending { it.first.second }
    }

    // Attached Proofs for this mode
    val transactionsWithProofs = remember(transactions) {
        transactions.filter { !it.proofImageUri.isNullOrBlank() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cashflow Summary Grid
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cash Movement Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (netCashflow >= 0) Color(0xFF00C853).copy(alpha = 0.15f) else Color(0xFFFF3B30).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (netCashflow >= 0) "Net Positive" else "Net Outflow",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (netCashflow >= 0) Color(0xFF00C853) else Color(0xFFFF3B30),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Inflow Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00C853).copy(alpha = 0.1f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                                    Text("Total Inflow", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00C853))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyUtils.formatCurrency(totalInflow, currencyCode, hideMoney = hideMoney),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00C853)
                                )
                            }
                        }

                        // Outflow Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF3B30).copy(alpha = 0.1f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                                    Text("Total Outflow", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF3B30))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyUtils.formatCurrency(totalOutflow, currencyCode, hideMoney = hideMoney),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF3B30)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Average Expense:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            CurrencyUtils.formatCurrency(avgExpense, currencyCode, hideMoney = hideMoney),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Logged Entries:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${transactions.size} transactions", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category Spending Breakdown
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Spending by Category (${categorySpending.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (categorySpending.isEmpty()) {
                        Text(
                            text = "No expense transactions recorded for this account yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categorySpending.forEach { (catInfo, catColorInt) ->
                            val (catName, amount, count) = catInfo
                            val percentage = if (totalOutflow > 0) (amount / totalOutflow).toFloat() else 0f
                            val categoryColor = Color(catColorInt)

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(categoryColor)
                                        )
                                        Text(
                                            text = "$catName ($count)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = CurrencyUtils.formatCurrency(amount, currencyCode, hideMoney = hideMoney) + " (${String.format(Locale.getDefault(), "%.1f", percentage * 100)}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { percentage.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = categoryColor,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Photo Proofs & Receipts Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Attached Proof Photos (${transactionsWithProofs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (transactionsWithProofs.isEmpty()) {
                        Text(
                            text = "No receipt proof photos attached to this mode's transactions. When logging an expense/income, take a pic as proof!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            transactionsWithProofs.take(4).forEach { tx ->
                                val file = File(tx.proofImageUri!!)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onViewProof(tx) }
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Receipt photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderCard(
    account: AccountEntity,
    parentAccount: AccountEntity? = null,
    childSubAccounts: List<AccountEntity> = emptyList(),
    hideMoney: Boolean = false,
    onEditClick: () -> Unit,
    onAdjustBalanceClick: (() -> Unit)? = null,
    onAddSubAccountClick: (() -> Unit)? = null,
    onToggleTally: ((Boolean) -> Unit)? = null,
    onOpenBankApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountColor = Color(account.color)
    val matchingApp = remember(account) {
        BankAppLauncher.findMatchingBankApp(account.name, account.linkedAppPackage)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = accountColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onEditClick() }
            .testTag("account_header_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = (account.subAccountType ?: account.type).uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (parentAccount != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "↳ of ${parentAccount.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Tally status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (account.includeInTally) Color.Black.copy(alpha = 0.25f) else Color(0xFFD32F2F).copy(alpha = 0.6f),
                        onClick = { onToggleTally?.invoke(!account.includeInTally) },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (account.includeInTally) Icons.Default.Check else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (account.includeInTally) Color(0xFF00E676) else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (account.includeInTally) "In Tally" else "Off Tally",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (account.isLocked || account.lockedUntil != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked Account",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(12.dp)
                                )
                                val isPastMaturity = account.lockedUntil?.before(Date()) == true
                                val lockLabel = if (account.lockedUntil != null) {
                                    if (isPastMaturity) {
                                        "Matured (${SimpleDateFormat("MMM d", Locale.getDefault()).format(account.lockedUntil)})"
                                    } else {
                                        "Locked till ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(account.lockedUntil)}"
                                    }
                                } else {
                                    "Locked"
                                }
                                Text(
                                    text = lockLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onAdjustBalanceClick != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.25f),
                            onClick = onAdjustBalanceClick,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Calibrate Balance",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Calibrate",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = account.currencyCode,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.25f),
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Account",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                if (onAdjustBalanceClick != null) {
                    Text(
                        text = "Tap balance to calibrate",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Text(
                text = CurrencyUtils.formatCurrency(account.currentBalance, account.currencyCode, hideMoney = hideMoney),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.ExtraBold,
                color = if (account.currentBalance < 0.0) Color(0xFFFF8A80) else Color.White,
                modifier = Modifier.clickable { onAdjustBalanceClick?.invoke() }
            )

            // Negative Balance Warning & Quick-Fix Banner
            if (account.currentBalance < 0.0 && onAdjustBalanceClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD32F2F).copy(alpha = 0.85f),
                    onClick = onAdjustBalanceClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Negative balance detected. Tap to calibrate real balance.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Fix Balance ➔",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            if (account.interestRatePa > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Yield: +${String.format(Locale.getDefault(), "%.2f", account.interestRatePa)}% p.a. (${account.compoundingFrequency.displayName})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // If this is a main account, show quick sub-accounts summary and add subaccount button
            if (account.parentAccountId == null && onAddSubAccountClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${childSubAccounts.size} Sub-Account${if (childSubAccounts.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        onClick = onAddSubAccountClick
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Sub-Account",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.3f),
                onClick = onOpenBankApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_header_open_bank_app")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (matchingApp != null) "Open ${matchingApp.displayName} App" else "Quick Go-To-App (${account.name})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Launch ↗",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Calibrate / Adjust Balance Dialog for immediate recovery and custom calibration.
 */
@Composable
fun CalibrateBalanceDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var balanceText by remember {
        mutableStateOf(
            if (account.currentBalance < 0.0) "" else {
                if (account.currentBalance % 1.0 == 0.0) account.currentBalance.toLong().toString() else account.currentBalance.toString()
            }
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Calibrate Balance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Set the actual current balance for '${account.name}'. All recorded transactions will be preserved, and the starting balance will be automatically calibrated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (account.currentBalance < 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Currently showing negative (${CurrencyUtils.formatCurrency(account.currentBalance, account.currencyCode)}) because expenses were logged without starting funds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it
                        errorMessage = null
                    },
                    label = { Text("Real Current Balance (${account.currencyCode})") },
                    placeholder = { Text("e.g. 5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = balanceText.toDoubleOrNull()
                    if (parsed == null) {
                        errorMessage = "Please enter a valid numeric amount"
                    } else {
                        onSave(parsed)
                    }
                }
            ) {
                Text("Save & Calibrate", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AccountTransactionItem(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    currencyCode: String,
    hideMoney: Boolean = false,
    onViewProof: (() -> Unit)? = null,
    onDeleteClick: () -> Unit
) {
    val isIncome = remember(transaction.type) { transaction.type.equals(TransactionType.INCOME.displayName, ignoreCase = true) }
    val isTransfer = remember(transaction.type) { transaction.type.equals(TransactionType.TRANSFER.displayName, ignoreCase = true) }

    val typeColor = remember(isIncome, isTransfer) {
        when {
            isIncome -> Color(0xFF00C853)
            isTransfer -> Color(0xFF0091EA)
            else -> Color(0xFFD50000)
        }
    }

    val typeIcon: ImageVector = remember(isIncome, isTransfer) {
        when {
            isIncome -> Icons.Default.ArrowDownward
            isTransfer -> Icons.Default.SwapHoriz
            else -> Icons.Default.ArrowUpward
        }
    }

    val formattedDate = remember(transaction.date) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(transaction.date)
    }

    val formattedAmount = remember(transaction.amount, currencyCode, hideMoney, isIncome, isTransfer) {
        val amt = CurrencyUtils.formatCurrency(transaction.amount, currencyCode, hideMoney = hideMoney)
        if (hideMoney) {
            amt
        } else {
            val sign = when {
                isIncome -> "+"
                isTransfer -> ""
                else -> "-"
            }
            "$sign$amt"
        }
    }

    val noteTitle = remember(transaction.note, category?.name, transaction.type) {
        transaction.note?.takeIf { it.isNotBlank() } ?: category?.name ?: transaction.type
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = noteTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!transaction.proofImageUri.isNullOrBlank()) {
                        val file = File(transaction.proofImageUri)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
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

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = typeColor
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
