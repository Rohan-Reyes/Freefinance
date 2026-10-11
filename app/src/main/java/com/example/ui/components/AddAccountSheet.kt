package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.model.AccountType
import com.example.data.local.model.CompoundingFrequency
import com.example.util.BankAppLauncher
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    defaultCurrency: String = "USD",
    initialAccount: AccountEntity? = null,
    allAccounts: List<AccountEntity> = emptyList(),
    preselectedParentAccountId: Long? = null,
    onDismissRequest: () -> Unit,
    onSaveAccount: (
        name: String,
        type: String,
        initialBalance: Double,
        color: Int,
        interestRatePa: Float,
        compoundingFrequency: CompoundingFrequency,
        currencyCode: String,
        isLocked: Boolean,
        lockedUntil: Date?,
        linkedAppPackage: String?,
        includeInTally: Boolean,
        parentAccountId: Long?,
        subAccountType: String?
    ) -> Unit,
    onDeleteAccount: ((AccountEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val presetColors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF009688), // Teal
        Color(0xFFE91E63)  // Pink
    )

    // Available potential parent accounts (only top-level accounts, excluding self)
    val availableParentAccounts = remember(allAccounts, initialAccount) {
        allAccounts.filter { it.parentAccountId == null && it.id != (initialAccount?.id ?: 0L) }
    }

    val initialIsSubAccount = remember(initialAccount, preselectedParentAccountId) {
        initialAccount?.parentAccountId != null || (preselectedParentAccountId != null && availableParentAccounts.isNotEmpty())
    }

    var isSubAccount by remember { mutableStateOf(initialIsSubAccount) }
    var selectedParentAccountId by remember(initialAccount, preselectedParentAccountId) {
        mutableStateOf<Long?>(
            initialAccount?.parentAccountId ?: preselectedParentAccountId ?: availableParentAccounts.firstOrNull()?.id
        )
    }
    var parentDropdownExpanded by remember { mutableStateOf(false) }
    var subAccountType by remember(initialAccount) {
        mutableStateOf(initialAccount?.subAccountType ?: "Savings")
    }

    var name by remember(initialAccount) { mutableStateOf(initialAccount?.name ?: "") }
    var initialBalanceText by remember(initialAccount) {
        mutableStateOf(
            initialAccount?.let {
                if (it.currentBalance % 1.0 == 0.0) it.currentBalance.toLong().toString() else it.currentBalance.toString()
            } ?: ""
        )
    }
    var selectedCurrency by remember(initialAccount, defaultCurrency) {
        mutableStateOf(initialAccount?.currencyCode ?: defaultCurrency)
    }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var selectedType by remember(initialAccount) {
        mutableStateOf(
            AccountType.values().find { it.displayName.equals(initialAccount?.type, ignoreCase = true) }
                ?: AccountType.BANK
        )
    }
    var linkedAppPackage by remember(initialAccount) {
        mutableStateOf(initialAccount?.linkedAppPackage ?: "")
    }
    var showAppSelector by remember(initialAccount) {
        mutableStateOf(!initialAccount?.linkedAppPackage.isNullOrBlank())
    }
    var showYieldSection by remember(initialAccount) {
        mutableStateOf((initialAccount?.interestRatePa ?: 0f) > 0f || (initialAccount?.subAccountType?.contains("Deposit", ignoreCase = true) == true) || (initialAccount?.subAccountType?.contains("Savings", ignoreCase = true) == true))
    }
    var interestRateText by remember(initialAccount) {
        mutableStateOf(
            if ((initialAccount?.interestRatePa ?: 0f) > 0f) initialAccount?.interestRatePa.toString() else ""
        )
    }
    var selectedCompounding by remember(initialAccount) {
        mutableStateOf(
            if (initialAccount != null && initialAccount.compoundingFrequency != CompoundingFrequency.NONE) {
                initialAccount.compoundingFrequency
            } else {
                CompoundingFrequency.MONTHLY
            }
        )
    }
    var isLocked by remember(initialAccount) {
        mutableStateOf(
            initialAccount?.isLocked == true ||
            initialAccount?.lockedUntil != null ||
            initialAccount?.type.equals(AccountType.INVESTMENT.displayName, ignoreCase = true) ||
            initialAccount?.subAccountType.equals("Time Deposit", ignoreCase = true)
        )
    }
    var lockedUntilDate by remember(initialAccount) {
        mutableStateOf<Date?>(initialAccount?.lockedUntil)
    }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedColorIndex by remember(initialAccount) {
        val initialIdx = if (initialAccount != null) {
            presetColors.indexOfFirst { it.toArgb() == initialAccount.color }.let { if (it >= 0) it else 0 }
        } else 0
        mutableIntStateOf(initialIdx)
    }
    var includeInTally by remember(initialAccount) {
        mutableStateOf(initialAccount?.includeInTally ?: true)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialAccount == null) {
                        if (isSubAccount) "New Sub-Account" else "New Account"
                    } else {
                        if (isSubAccount) "Edit Sub-Account" else "Edit Account"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (initialAccount != null && onDeleteAccount != null) {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Account",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }

            // Account Structure Toggle (Main Account vs Sub-Account)
            if (availableParentAccounts.isNotEmpty() && initialAccount == null) {
                Text(
                    text = "ACCOUNT HIERARCHY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isSubAccount,
                        onClick = { isSubAccount = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Main Account", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = isSubAccount,
                        onClick = {
                            isSubAccount = true
                            if (selectedParentAccountId == null) {
                                selectedParentAccountId = availableParentAccounts.firstOrNull()?.id
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sub-Account", fontSize = 12.sp)
                    }
                }
            }

            // Sub-Account Parent & Preset Configuration
            AnimatedVisibility(visible = isSubAccount) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val parentAccount = availableParentAccounts.find { it.id == selectedParentAccountId }

                    ExposedDropdownMenuBox(
                        expanded = parentDropdownExpanded,
                        onExpandedChange = { parentDropdownExpanded = !parentDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = parentAccount?.let { "${it.name} (${it.type})" } ?: "Select Parent Account",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Parent Main Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = parentDropdownExpanded,
                            onDismissRequest = { parentDropdownExpanded = false }
                        ) {
                            availableParentAccounts.forEach { parent ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(parent.color))
                                            )
                                            Text("${parent.name} (${parent.type} • ${parent.currencyCode})")
                                        }
                                    },
                                    onClick = {
                                        selectedParentAccountId = parent.id
                                        selectedCurrency = parent.currencyCode
                                        if (parent.linkedAppPackage != null) {
                                            linkedAppPackage = parent.linkedAppPackage
                                        }
                                        parentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "SUB-ACCOUNT PURPOSE / CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val subPresets = listOf(
                            "Savings",
                            "Time Deposit",
                            "Wallet",
                            "Checking",
                            "Emergency Fund",
                            "Investment"
                        )
                        subPresets.forEach { preset ->
                            val isSelected = subAccountType.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    subAccountType = preset
                                    if (name.isBlank() || subPresets.contains(name)) {
                                        name = preset
                                    }
                                    when (preset) {
                                        "Time Deposit" -> {
                                            isLocked = true
                                            showYieldSection = true
                                            selectedType = AccountType.INVESTMENT
                                            if (lockedUntilDate == null) {
                                                val cal = Calendar.getInstance()
                                                cal.add(Calendar.DAY_OF_YEAR, 30)
                                                lockedUntilDate = cal.time
                                            }
                                        }
                                        "Savings" -> {
                                            showYieldSection = true
                                            selectedType = AccountType.BANK
                                        }
                                        "Wallet" -> {
                                            selectedType = AccountType.E_WALLET
                                        }
                                        "Investment" -> {
                                            selectedType = AccountType.INVESTMENT
                                            showYieldSection = true
                                        }
                                        else -> {
                                            selectedType = AccountType.BANK
                                        }
                                    }
                                },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Account Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        if (isSubAccount) "Sub-Account Name (e.g. Maya Savings, BPI Time Deposit)"
                        else "Account Name (e.g., Primary Checking, Cash, Maya)"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_name_input")
            )

            // Balance & Currency Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text(if (initialAccount == null) "Initial Balance" else "Current Balance") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("account_balance_input")
                )

                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded },
                    modifier = Modifier.width(110.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("account_currency_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        CurrencyUtils.SUPPORTED_CURRENCIES.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text("${curr.code} (${curr.symbol})") },
                                onClick = {
                                    selectedCurrency = curr.code
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Account Type / Mode Selector
            Text(
                text = "ACCOUNT TYPE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(AccountType.BANK, AccountType.CASH, AccountType.E_WALLET, AccountType.INVESTMENT)
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            if (type == AccountType.INVESTMENT) {
                                showYieldSection = true
                                isLocked = true
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(
                            text = when (type) {
                                AccountType.BANK -> "Bank"
                                AccountType.CASH -> "Cash"
                                AccountType.E_WALLET -> "E-Wallet"
                                AccountType.INVESTMENT -> "Deposit"
                                else -> type.displayName
                            },
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Accent Color Chooser
            Text(
                text = "BRAND ACCENT COLOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetColors.forEachIndexed { index, color ->
                    val isSelected = selectedColorIndex == index
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColorIndex = index }
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Locked Funds / Time Deposit Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Is Locked (Time Deposit / Vault)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hold funds until maturity date (e.g. Maya Time Deposit)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isLocked,
                            onCheckedChange = { checked ->
                                isLocked = checked
                                if (checked && lockedUntilDate == null) {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, 30)
                                    lockedUntilDate = cal.time
                                }
                            }
                        )
                    }

                    AnimatedVisibility(visible = isLocked) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "QUICK MATURITY PRESETS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    "30d" to 30,
                                    "90d" to 90,
                                    "180d" to 180,
                                    "1 Year" to 365
                                )
                                presets.forEach { (label, days) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.DAY_OF_YEAR, days)
                                            lockedUntilDate = cal.time
                                        },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Selected date display card
                            OutlinedCard(
                                onClick = { showDatePickerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Locked Till (Maturity Date)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val formattedDate = lockedUntilDate?.let {
                                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
                                            } ?: "Indefinite Lock"
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (lockedUntilDate != null) {
                                            TextButton(onClick = { lockedUntilDate = null }) {
                                                Text("Indefinite", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        TextButton(onClick = { showDatePickerDialog = true }) {
                                            Text("Set Date", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Yield / Interest Settings
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showYieldSection = !showYieldSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Percent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Yield / Interest Settings (P.A.)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Forecast compounding returns (e.g., 4.5% or 6.0% p.a.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showYieldSection) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = interestRateText,
                                onValueChange = { interestRateText = it },
                                label = { Text("Annual Interest Rate (P.A. %)") },
                                placeholder = { Text("e.g., 3.5, 4.5, or 6.0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Compounding Frequency",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val frequencies = listOf(
                                    CompoundingFrequency.DAILY,
                                    CompoundingFrequency.MONTHLY,
                                    CompoundingFrequency.ANNUALLY
                                )
                                frequencies.forEachIndexed { index, freq ->
                                    SegmentedButton(
                                        selected = selectedCompounding == freq,
                                        onClick = { selectedCompounding = freq },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = frequencies.size)
                                    ) {
                                        Text(freq.displayName, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick App Link Section (MariBank, GCash, Maya, SeaBank, etc.)
            val detectedApp = remember(name) { BankAppLauncher.findMatchingBankApp(name) }
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppSelector = !showAppSelector },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Quick Go-To-App Button",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (linkedAppPackage.isNotBlank()) "Linked: $linkedAppPackage" else (detectedApp?.let { "Detected: ${it.displayName}" } ?: "Open bank/wallet app with 1 tap"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle app selector",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showAppSelector || detectedApp != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (detectedApp != null && linkedAppPackage != detectedApp.packageName) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    onClick = { linkedAppPackage = detectedApp.packageName },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Auto-link to ${detectedApp.displayName} (${detectedApp.packageName})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Popular Bank & Wallet Apps:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val popularPresets = listOf(
                                    "MariBank" to "com.maribank.ph",
                                    "GCash" to "com.globe.gcash.android",
                                    "Maya" to "com.voyagerinnovation.paymaya",
                                    "SeaBank" to "ph.com.seabank.mobile",
                                    "BPI" to "com.bpi.ng.app",
                                    "BDO Pay" to "com.bdo.pay",
                                    "UnionBank" to "com.unionbankph.online",
                                    "GoTyme" to "ph.gotyme.app",
                                    "PayPal" to "com.paypal.android.p2pmobile",
                                    "Wise" to "com.transferwise.android"
                                )
                                popularPresets.forEach { (appName, pkg) ->
                                    val isSelected = linkedAppPackage == pkg
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            linkedAppPackage = if (isSelected) "" else pkg
                                        },
                                        label = { Text(appName, fontSize = 11.sp) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = linkedAppPackage,
                                onValueChange = { linkedAppPackage = it },
                                label = { Text("App Package Name") },
                                placeholder = { Text("e.g., com.maribank.ph") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Tally / Total Net Worth Inclusion Setting
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (includeInTally) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeInTally = !includeInTally }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (includeInTally) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (includeInTally) Icons.Default.Check else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (includeInTally) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Include in Total Balance / Tally",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (includeInTally)
                                    "Counted in Unified Net Worth, Hero Balance, & widget totals"
                                else
                                    "Excluded from total tally — tracked separately as auxiliary/segregated funds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = includeInTally,
                        onCheckedChange = { includeInTally = it }
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter an account name"
                        return@Button
                    }
                    val balanceVal = initialBalanceText.toDoubleOrNull() ?: 0.0
                    val interestRate = interestRateText.toFloatOrNull() ?: 0.0f
                    val chosenColor = presetColors[selectedColorIndex].toArgb()

                    // If linkedAppPackage is empty, attempt auto-detect
                    val finalAppPackage = linkedAppPackage.ifBlank {
                        BankAppLauncher.findMatchingBankApp(name)?.packageName
                    }

                    val finalParentAccountId = if (isSubAccount) selectedParentAccountId else null
                    val finalSubAccountType = if (isSubAccount) subAccountType else null

                    onSaveAccount(
                        name.trim(),
                        selectedType.displayName,
                        balanceVal,
                        chosenColor,
                        interestRate,
                        if (interestRate > 0) selectedCompounding else CompoundingFrequency.NONE,
                        selectedCurrency,
                        isLocked,
                        if (isLocked) lockedUntilDate else null,
                        finalAppPackage,
                        includeInTally,
                        finalParentAccountId,
                        finalSubAccountType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_account_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (initialAccount == null) {
                        if (isSubAccount) "Create Sub-Account" else "Create Account"
                    } else "Save Changes",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = lockedUntilDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            lockedUntilDate = Date(it)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirmDialog && initialAccount != null && onDeleteAccount != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (initialAccount.parentAccountId != null) "Delete Sub-Account?" else "Delete Account?") },
            text = { Text("Are you sure you want to delete '${initialAccount.name}'? Transactions associated with this account will remain.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount(initialAccount)
                        showDeleteConfirmDialog = false
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
}
