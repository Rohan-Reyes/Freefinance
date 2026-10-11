package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.model.DebtType
import com.example.ui.components.EmptyListPlaceholder
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class DebtFilterOption(val label: String) {
    ALL("All"),
    DUE_SOON("Due Soon (< 7d)"),
    OVERDUE("Overdue"),
    NO_DUE_DATE("No Date")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: "Owe Me" (Lent), 1: "I Owe" (Borrowed)
    val uiState by viewModel.uiState.collectAsState()
    val baseCurrency = uiState.baseCurrency
    val lentDebts by viewModel.activeLentDebts.collectAsState()
    val borrowedDebts by viewModel.activeBorrowedDebts.collectAsState()
    val totalLent by viewModel.totalActiveLent.collectAsState()
    val totalBorrowed by viewModel.totalActiveBorrowed.collectAsState()

    var activeFilter by remember { mutableStateOf(DebtFilterOption.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }
    var debtToSettle by remember { mutableStateOf<DebtEntity?>(null) }
    val allAccountsList by viewModel.allAccounts.collectAsState()

    val rawList = if (selectedTabIndex == 0) lentDebts else borrowedDebts
    val currentTotal = if (selectedTabIndex == 0) totalLent else totalBorrowed

    // Filtered list based on expected payout date
    val currentList by remember(rawList, activeFilter) {
        derivedStateOf {
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val sevenDaysFromNow = now + 7L * 24 * 60 * 60 * 1000

            when (activeFilter) {
                DebtFilterOption.ALL -> rawList
                DebtFilterOption.DUE_SOON -> rawList.filter { debt ->
                    debt.dueDate != null && debt.dueDate.time in now..sevenDaysFromNow
                }
                DebtFilterOption.OVERDUE -> rawList.filter { debt ->
                    debt.dueDate != null && debt.dueDate.time < now
                }
                DebtFilterOption.NO_DUE_DATE -> rawList.filter { debt ->
                    debt.dueDate == null
                }
            }
        }
    }

    // Payout stats for current category
    val (upcomingCount, overdueCount) = remember(rawList) {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val upcoming = rawList.count { it.dueDate != null && it.dueDate.time >= now }
        val overdue = rawList.count { it.dueDate != null && it.dueDate.time < now }
        upcoming to overdue
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Lent & Borrowed", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row: "Owe Me" / "I Owe"
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        activeFilter = DebtFilterOption.ALL
                    },
                    text = {
                        Text(
                            "Owe Me (${lentDebts.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        activeFilter = DebtFilterOption.ALL
                    },
                    text = {
                        Text(
                            "I Owe (${borrowedDebts.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Overview Metric Card with Expected Payout Indicators
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTabIndex == 0) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFD50000).copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedTabIndex == 0) "TOTAL MONEY OWED TO YOU" else "TOTAL YOU OWE OTHERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 0) Color(0xFF00C853) else Color(0xFFD50000)
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(currentTotal, baseCurrency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTabIndex == 0) Color(0xFF00C853) else Color(0xFFD50000)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedTabIndex == 0) Color(0xFF00C853).copy(alpha = 0.2f) else Color(0xFFD50000).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = if (selectedTabIndex == 0) Color(0xFF00C853) else Color(0xFFD50000)
                            )
                        }
                    }

                    // Payout Schedule Subtitle
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$upcomingCount scheduled payouts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (overdueCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$overdueCount overdue",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DebtFilterOption.entries) { option ->
                    FilterChip(
                        selected = activeFilter == option,
                        onClick = { activeFilter = option },
                        label = { Text(option.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // List of Debts
            if (currentList.isEmpty()) {
                EmptyListPlaceholder(
                    icon = Icons.Default.People,
                    title = when (activeFilter) {
                        DebtFilterOption.ALL -> if (selectedTabIndex == 0) "No Pending Receivables" else "No Outstanding Payables"
                        DebtFilterOption.DUE_SOON -> "No Payouts Due Soon"
                        DebtFilterOption.OVERDUE -> "No Overdue Payouts"
                        DebtFilterOption.NO_DUE_DATE -> "No Debts Without Due Dates"
                    },
                    message = if (selectedTabIndex == 0) "No one currently owes you money under this filter." else "You don't owe any money to anyone right now under this filter.",
                    actionLabel = "Record Person / Debt",
                    onAction = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList, key = { it.id }) { debt ->
                        DebtItemCard(
                            debt = debt,
                            isLent = selectedTabIndex == 0,
                            currencyCode = baseCurrency,
                            onEditClicked = {
                                debtToEdit = debt
                            },
                            onSettleClicked = {
                                debtToSettle = debt
                            },
                            onDeleteClicked = {
                                viewModel.deleteDebt(debt)
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit Debt Dialog
    debtToEdit?.let { debt ->
        EditDebtDialog(
            debt = debt,
            currencyCode = baseCurrency,
            onDismiss = { debtToEdit = null },
            onConfirm = { name, type, totalAmount, remainingAmount, dueDate, notes ->
                viewModel.updateDebt(
                    debtId = debt.id,
                    personName = name,
                    type = type,
                    totalAmount = totalAmount,
                    remainingAmount = remainingAmount,
                    dueDate = dueDate,
                    notes = notes
                )
                debtToEdit = null
            }
        )
    }

    // Add Debt Dialog
    if (showAddDialog) {
        AddDebtDialog(
            initialType = if (selectedTabIndex == 0) DebtType.LENT else DebtType.BORROWED,
            currencyCode = baseCurrency,
            accounts = allAccountsList,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, amount, dueDate, notes, accountId ->
                viewModel.addDebt(name, type, amount, dueDate, notes, accountId)
                showAddDialog = false
            }
        )
    }

    // Settle / Pay Dialog (with Account Selection & Balance Updates)
    debtToSettle?.let { debt ->
        SettleDebtDialog(
            debt = debt,
            currencyCode = baseCurrency,
            accounts = allAccountsList,
            onDismiss = { debtToSettle = null },
            onConfirm = { amount, accountId ->
                if (amount >= debt.remainingAmount) {
                    viewModel.settleDebt(debt.id, accountId)
                } else {
                    viewModel.payDebtPortion(debt.id, amount, accountId)
                }
                debtToSettle = null
            }
        )
    }
}

@Composable
private fun DebtItemCard(
    debt: DebtEntity,
    isLent: Boolean,
    currencyCode: String = "USD",
    onEditClicked: () -> Unit,
    onSettleClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Payout date calculation
    val payoutStatus = remember(debt.dueDate) {
        val due = debt.dueDate
        if (due == null) {
            null
        } else {
            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dueCal = Calendar.getInstance().apply {
                time = due
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMillis = dueCal.timeInMillis - nowCal.timeInMillis
            val diffDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt()

            when {
                diffDays < 0 -> Pair("Overdue by ${-diffDays}d", Color(0xFFD50000))
                diffDays == 0 -> Pair("Expected Today", Color(0xFFFF9100))
                diffDays == 1 -> Pair("Expected Tomorrow", Color(0xFF00B0FF))
                diffDays <= 7 -> Pair("Due in ${diffDays}d", Color(0xFF2979FF))
                else -> Pair("Due in ${diffDays}d", Color.Gray)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Created ${dateFormat.format(debt.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatCurrency(debt.remainingAmount, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLent) Color(0xFF00C853) else Color(0xFFD50000)
                    )
                    if (debt.remainingAmount < debt.totalAmount) {
                        Text(
                            text = "of " + CurrencyUtils.formatCurrency(debt.totalAmount, currencyCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expected Day of Payout Indicator Badge
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (debt.dueDate != null) {
                    if (payoutStatus?.first?.startsWith("Overdue") == true) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    }
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (debt.dueDate != null) Icons.Default.Event else Icons.Default.Schedule,
                            contentDescription = "Payout Day",
                            modifier = Modifier.size(14.dp),
                            tint = if (payoutStatus?.first?.startsWith("Overdue") == true) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = if (debt.dueDate != null) {
                                (if (isLent) "Expected Payout: " else "Expected Repayment: ") + dateFormat.format(debt.dueDate)
                            } else {
                                "No expected payout date set"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (debt.dueDate != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (payoutStatus?.first?.startsWith("Overdue") == true) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (payoutStatus != null) {
                        Text(
                            text = payoutStatus.first,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = payoutStatus.second
                        )
                    }
                }
            }

            if (!debt.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = debt.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEditClicked) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSettleClicked,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLent) Color(0xFF00C853) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isLent) "Receive / Settle" else "Pay / Settle")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDebtDialog(
    debt: DebtEntity,
    currencyCode: String = "USD",
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: DebtType, totalAmount: Double, remainingAmount: Double, dueDate: Date?, notes: String?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var personName by remember { mutableStateOf(debt.personName) }
    var totalAmountText by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", debt.totalAmount)) }
    var remainingAmountText by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", debt.remainingAmount)) }
    var selectedType by remember {
        mutableStateOf(if (debt.type.equals("Borrowed", ignoreCase = true)) DebtType.BORROWED else DebtType.LENT)
    }
    var selectedDueDate by remember { mutableStateOf<Date?>(debt.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(debt.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currencySymbol = CurrencyUtils.getSymbol(currencyCode)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDueDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDueDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Debt Record") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == DebtType.LENT,
                        onClick = { selectedType = DebtType.LENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("I Lent")
                    }
                    SegmentedButton(
                        selected = selectedType == DebtType.BORROWED,
                        onClick = { selectedType = DebtType.BORROWED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("I Borrowed")
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("Total Initial Amount") },
                    prefix = { Text("$currencySymbol ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remainingAmountText,
                    onValueChange = { remainingAmountText = it },
                    label = { Text("Remaining Unpaid Amount") },
                    prefix = { Text("$currencySymbol ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Expected Day of Payout selector
                Text(
                    text = "Expected Day of Payout",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = selectedDueDate?.let { dateFormat.format(it) } ?: "Set Expected Payout Date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedDueDate != null) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (selectedDueDate != null) {
                            IconButton(
                                onClick = { selectedDueDate = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Date", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Quick preset timeline chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                            selectedDueDate = cal.time
                        },
                        label = { Text("+7 Days", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }
                            selectedDueDate = cal.time
                        },
                        label = { Text("+14 Days", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
                            selectedDueDate = cal.time
                        },
                        label = { Text("+30 Days", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            val now = Calendar.getInstance()
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                if (now.get(Calendar.DAY_OF_MONTH) >= maxDay) {
                                    add(Calendar.MONTH, 1)
                                }
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            selectedDueDate = cal.time
                        },
                        label = { Text("End of Month", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason") },
                    singleLine = true,
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
                    val total = totalAmountText.toDoubleOrNull()
                    val remaining = remainingAmountText.toDoubleOrNull()
                    if (personName.isBlank()) {
                        errorMessage = "Please enter a name"
                        return@Button
                    }
                    if (total == null || total <= 0) {
                        errorMessage = "Please enter a valid total amount"
                        return@Button
                    }
                    if (remaining == null || remaining < 0) {
                        errorMessage = "Please enter a valid remaining amount"
                        return@Button
                    }
                    if (remaining > total) {
                        errorMessage = "Remaining cannot exceed total amount"
                        return@Button
                    }
                    onConfirm(personName.trim(), selectedType, total, remaining, selectedDueDate, notes.ifBlank { null })
                }
            ) {
                Text("Update Debt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtDialog(
    initialType: DebtType,
    currencyCode: String = "USD",
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: DebtType, amount: Double, dueDate: Date?, notes: String?, accountId: Long?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedDueDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var reflectInAccount by remember { mutableStateOf(accounts.isNotEmpty()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currencySymbol = CurrencyUtils.getSymbol(currencyCode)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDueDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDueDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Lent / Borrowed") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == DebtType.LENT,
                        onClick = { selectedType = DebtType.LENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("I Lent (Owed to Me)")
                    }
                    SegmentedButton(
                        selected = selectedType == DebtType.BORROWED,
                        onClick = { selectedType = DebtType.BORROWED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("I Borrowed")
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    prefix = { Text("$currencySymbol ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Reflection Toggle
                if (accounts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (selectedType == DebtType.LENT) "Deduct from Account now (Cash Out)" else "Deposit into Account now (Loan In)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Checkbox(
                                    checked = reflectInAccount,
                                    onCheckedChange = { reflectInAccount = it }
                                )
                            }

                            if (reflectInAccount) {
                                val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.first()
                                ExposedDropdownMenuBox(
                                    expanded = accountDropdownExpanded,
                                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = "${selectedAccount.name} (${CurrencyUtils.formatCurrency(selectedAccount.currentBalance, selectedAccount.currencyCode)})",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(if (selectedType == DebtType.LENT) "Source Account (Deduct from)" else "Destination Account (Deposit to)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = accountDropdownExpanded,
                                        onDismissRequest = { accountDropdownExpanded = false }
                                    ) {
                                        accounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = if (acc.isSubAccount) "  ↳ ${acc.name} (${acc.subAccountType ?: acc.type})" else acc.name,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "${acc.type} • Balance: ${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedAccountId = acc.id
                                                    accountDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expected Day of Payout Section
                Text(
                    text = "Expected Day of Payout",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = selectedDueDate?.let { dateFormat.format(it) } ?: "Set Expected Payout Date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedDueDate != null) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (selectedDueDate != null) {
                            IconButton(
                                onClick = { selectedDueDate = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Date", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Quick preset timeline chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                            selectedDueDate = cal.time
                        },
                        label = { Text("+7 Days", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }
                            selectedDueDate = cal.time
                        },
                        label = { Text("+14 Days", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
                            selectedDueDate = cal.time
                        },
                        label = { Text("+30 Days", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            val now = Calendar.getInstance()
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                if (now.get(Calendar.DAY_OF_MONTH) >= maxDay) {
                                    add(Calendar.MONTH, 1)
                                }
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            selectedDueDate = cal.time
                        },
                        label = { Text("End of Month", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Dinner, Concert ticket)") },
                    singleLine = true,
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
                    val amount = amountText.toDoubleOrNull()
                    if (personName.isBlank()) {
                        errorMessage = "Please enter a name"
                        return@Button
                    }
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid positive amount"
                        return@Button
                    }
                    val targetAccId = if (reflectInAccount) selectedAccountId else null
                    onConfirm(personName.trim(), selectedType, amount, selectedDueDate, notes.ifBlank { null }, targetAccId)
                }
            ) {
                Text("Save Debt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleDebtDialog(
    debt: DebtEntity,
    currencyCode: String = "USD",
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, accountId: Long?) -> Unit
) {
    val isLent = debt.type.equals("Lent", ignoreCase = true)
    val currencySymbol = CurrencyUtils.getSymbol(currencyCode)
    var settleAmountText by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", debt.remainingAmount)) }
    var reflectInAccount by remember { mutableStateOf(accounts.isNotEmpty()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isLent) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isLent) Color(0xFF00C853) else Color(0xFFD50000)
                )
                Text(
                    text = if (isLent) "Receive Payment: ${debt.personName}" else "Repay Debt: ${debt.personName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLent) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFD50000).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isLent) "CURRENT RECEIVABLE" else "OUTSTANDING BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLent) Color(0xFF00C853) else Color(0xFFD50000)
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(debt.remainingAmount, currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLent) Color(0xFF00C853) else Color(0xFFD50000)
                            )
                        }
                        if (debt.remainingAmount < debt.totalAmount) {
                            Text(
                                text = "Total: " + CurrencyUtils.formatCurrency(debt.totalAmount, currencyCode),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Payment Amount input
                OutlinedTextField(
                    value = settleAmountText,
                    onValueChange = {
                        settleAmountText = it
                        errorMessage = null
                    },
                    label = { Text(if (isLent) "Amount Received" else "Amount Paid") },
                    prefix = { Text("$currencySymbol ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick preset amount chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = {
                            settleAmountText = String.format(Locale.getDefault(), "%.2f", debt.remainingAmount)
                        },
                        label = { Text("Full (${CurrencyUtils.formatCurrency(debt.remainingAmount, currencyCode)})", fontSize = 11.sp) }
                    )
                    if (debt.remainingAmount > 1.0) {
                        AssistChip(
                            onClick = {
                                val half = debt.remainingAmount / 2.0
                                settleAmountText = String.format(Locale.getDefault(), "%.2f", half)
                            },
                            label = { Text("50% (${CurrencyUtils.formatCurrency(debt.remainingAmount / 2.0, currencyCode)})", fontSize = 11.sp) }
                        )
                    }
                }

                // Target Account Selector
                if (accounts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isLent) "Receive into Account" else "Deduct from Account",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Checkbox(
                                    checked = reflectInAccount,
                                    onCheckedChange = { reflectInAccount = it }
                                )
                            }

                            if (reflectInAccount) {
                                val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.first()
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = "${selectedAccount.name} (${CurrencyUtils.formatCurrency(selectedAccount.currentBalance, selectedAccount.currencyCode)})",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(if (isLent) "Deposit Destination Account" else "Payment Source Account") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        accounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = if (acc.isSubAccount) "  ↳ ${acc.name} (${acc.subAccountType ?: acc.type})" else acc.name,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "${acc.type} • Balance: ${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedAccountId = acc.id
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isLent) {
                                        "✓ Money will be credited to ${selectedAccount.name} and logged in master records."
                                    } else {
                                        "✓ Money will be debited from ${selectedAccount.name} and logged in master records."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isLent) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

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
                    val amount = settleAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    val targetAccId = if (reflectInAccount) selectedAccountId else null
                    onConfirm(amount, targetAccId)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLent) Color(0xFF00C853) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isLent) "Confirm Received" else "Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

