package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.PlannedPayoutEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlannedPayoutSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    baseCurrency: String,
    initialPayout: PlannedPayoutEntity? = null,
    onDismissRequest: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        currencyCode: String,
        dueDate: Date,
        targetAccountId: Long?,
        categoryId: Long?,
        payeeOrVendor: String?,
        notes: String?,
        priority: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialPayout?.title ?: "") }
    var amountText by remember { mutableStateOf(if (initialPayout != null) initialPayout.amount.toString() else "") }
    var payee by remember { mutableStateOf(initialPayout?.payeeOrVendor ?: "") }
    var notes by remember { mutableStateOf(initialPayout?.notes ?: "") }
    var selectedAccountId by remember { mutableStateOf<Long?>(initialPayout?.targetAccountId ?: accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(initialPayout?.categoryId) }
    var priority by remember { mutableStateOf(initialPayout?.priority ?: "NORMAL") }
    var selectedDate by remember { mutableStateOf(initialPayout?.dueDate ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }.time) }

    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        text = if (initialPayout == null) "Plan Payout / Commitment" else "Edit Planned Payout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Schedule a one-off upcoming disbursement or commitment (not recurring subscription or debt).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (isError) isError = false
                },
                label = { Text("Payout Title / Purpose *") },
                placeholder = { Text("e.g. Annual Insurance Premium, Q3 Taxes, Laptop") },
                singleLine = true,
                isError = isError && title.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    if (isError) isError = false
                },
                label = { Text("Amount ($baseCurrency) *") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = isError && (amountText.toDoubleOrNull() == null || amountText.toDoubleOrNull()!! <= 0.0),
                modifier = Modifier.fillMaxWidth()
            )

            // Due Date Selector
            OutlinedTextField(
                value = sdf.format(selectedDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Date *") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // Quick Date chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Tomorrow" to 1,
                    "In 3 Days" to 3,
                    "In 1 Week" to 7,
                    "In 30 Days" to 30
                ).forEach { (label, days) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_MONTH, days)
                            selectedDate = cal.time
                        },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            // Priority Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Priority Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HIGH", "NORMAL", "LOW").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            // Source Account Selection
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedAcc = accounts.find { it.id == selectedAccountId }
                OutlinedTextField(
                    value = selectedAcc?.let { "${it.name} (${it.currencyCode})" } ?: "Any / Unassigned",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Source Account to Disburse From") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Any / Unassigned") },
                        onClick = {
                            selectedAccountId = null
                            accountDropdownExpanded = false
                        }
                    )
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.name} (${acc.currencyCode})") },
                            onClick = {
                                selectedAccountId = acc.id
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Category Selection
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedCat = categories.find { it.id == selectedCategoryId }
                OutlinedTextField(
                    value = selectedCat?.name ?: "Optional Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expense Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None / Uncategorized") },
                        onClick = {
                            selectedCategoryId = null
                            categoryDropdownExpanded = false
                        }
                    )
                    categories.filter { it.type.equals("Expense", ignoreCase = true) }.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Payee / Vendor
            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it },
                label = { Text("Payee / Vendor / Recipient (Optional)") },
                placeholder = { Text("e.g. AXA Insurance, Bureau of Internal Revenue") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Reference No. (Optional)") },
                placeholder = { Text("e.g. Policy #99281, Invoice #402") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (title.isBlank() || parsedAmount == null || parsedAmount <= 0.0) {
                        isError = true
                        return@Button
                    }
                    onSave(
                        title.trim(),
                        parsedAmount,
                        baseCurrency,
                        selectedDate,
                        selectedAccountId,
                        selectedCategoryId,
                        payee.trim().takeIf { it.isNotBlank() },
                        notes.trim().takeIf { it.isNotBlank() },
                        priority
                    )
                    onDismissRequest()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (initialPayout == null) "Schedule Planned Payout" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
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
}
