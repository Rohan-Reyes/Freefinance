package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.util.ReceiptStorageHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    baseCurrency: String = "USD",
    onDismissRequest: () -> Unit,
    onSaveTransaction: (updatedTransaction: TransactionEntity) -> Unit,
    onDeleteTransaction: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", transaction.amount)) }
    val initialTypeIndex = when (transaction.type.lowercase(Locale.ROOT)) {
        TransactionType.INCOME.displayName.lowercase(Locale.ROOT) -> 0
        TransactionType.EXPENSE.displayName.lowercase(Locale.ROOT) -> 1
        TransactionType.TRANSFER.displayName.lowercase(Locale.ROOT) -> 2
        else -> 1
    }
    var selectedTypeIndex by remember { mutableIntStateOf(initialTypeIndex) }
    val transactionTypes = listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER)
    val selectedType = transactionTypes[selectedTypeIndex]

    var selectedAccountId by remember { mutableStateOf<Long?>(transaction.accountId) }
    var selectedToAccountId by remember { mutableStateOf<Long?>(transaction.toAccountId) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(transaction.categoryId) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var labelsText by remember { mutableStateOf(transaction.labels.joinToString(", ")) }

    var proofImageUri by remember { mutableStateOf<String?>(transaction.proofImageUri) }
    var showProofViewer by remember { mutableStateOf(false) }
    var currentTempPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentTempPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    proofImageUri = file.absolutePath
                    Toast.makeText(context, "Proof photo updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ReceiptStorageHelper.saveUriToReceiptFile(context, uri)
            if (savedPath != null) {
                proofImageUri = savedPath
                Toast.makeText(context, "Proof photo selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var accountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currencySymbol = CurrencyUtils.getSymbol(baseCurrency)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDeleteTransaction,
                    modifier = Modifier.testTag("edit_tx_delete_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Transaction Type Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                transactionTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = transactionTypes.size),
                        onClick = { selectedTypeIndex = index },
                        selected = index == selectedTypeIndex,
                        icon = {
                            when (type) {
                                TransactionType.INCOME -> Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                                TransactionType.EXPENSE -> Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                                TransactionType.TRANSFER -> Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    ) {
                        Text(
                            text = type.displayName,
                            fontWeight = if (index == selectedTypeIndex) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Amount Input Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Account Selector
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentAccount = accounts.find { it.id == selectedAccountId }
                OutlinedTextField(
                    value = currentAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (selectedType == TransactionType.TRANSFER) "From Account" else "Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { acc ->
                        val accLabel = if (acc.parentAccountId != null) {
                            "↳ ${acc.name} [${acc.subAccountType ?: "Sub"}] (${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)})"
                        } else {
                            "${acc.name} (${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)})"
                        }
                        DropdownMenuItem(
                            text = { Text(accLabel) },
                            onClick = {
                                selectedAccountId = acc.id
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            // Destination Account for Transfer
            if (selectedType == TransactionType.TRANSFER) {
                ExposedDropdownMenuBox(
                    expanded = toAccountExpanded,
                    onExpandedChange = { toAccountExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val destAccount = accounts.find { it.id == selectedToAccountId }
                    OutlinedTextField(
                        value = destAccount?.name ?: "Select Destination Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toAccountExpanded,
                        onDismissRequest = { toAccountExpanded = false }
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            val destLabel = if (acc.parentAccountId != null) {
                                "↳ ${acc.name} [${acc.subAccountType ?: "Sub"}] (${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)})"
                            } else {
                                "${acc.name} (${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)})"
                            }
                            DropdownMenuItem(
                                text = { Text(destLabel) },
                                onClick = {
                                    selectedToAccountId = acc.id
                                    toAccountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Category Selector (for Income & Expense)
            if (selectedType != TransactionType.TRANSFER) {
                val filteredCategories = categories.filter { it.type == selectedType.displayName }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentCategory = categories.find { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = currentCategory?.name ?: "No Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedCategoryId = null
                                categoryExpanded = false
                            }
                        )
                        filteredCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Note / Description Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Description (e.g. Freelance Milestone 1)") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Labels / Tags
            OutlinedTextField(
                value = labelsText,
                onValueChange = { labelsText = it },
                label = { Text("Tags / Labels (comma-separated, e.g. Freelance, UI/UX)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Photo Proof / Receipt Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Photo Proof / Receipt",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (proofImageUri != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.clickable {
                                    ReceiptStorageHelper.deleteReceiptFile(proofImageUri)
                                    proofImageUri = null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Remove",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (proofImageUri != null) {
                        val imgFile = File(proofImageUri!!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showProofViewer = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = imgFile,
                                contentDescription = "Receipt proof",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Proof Attached (Tap to View)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00C853)
                                )
                                Text(
                                    text = imgFile.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val file = ReceiptStorageHelper.createTempReceiptFile(context)
                                    val uri = ReceiptStorageHelper.getUriForFile(context, file)
                                    currentTempPhotoFile = file
                                    cameraLauncher.launch(uri)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Take Pic", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Gallery", style = MaterialTheme.typography.labelMedium)
                                }
                            }
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

            // Save and Cancel Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorMessage = "Please enter a valid amount greater than 0"
                            return@Button
                        }
                        val accId = selectedAccountId
                        if (accId == null) {
                            errorMessage = "Please select an account"
                            return@Button
                        }
                        if (selectedType == TransactionType.TRANSFER && (selectedToAccountId == null || selectedToAccountId == accId)) {
                            errorMessage = "Please select a different destination account"
                            return@Button
                        }

                        val parsedLabels = labelsText
                            .split(",")
                            .map { it.trim().removePrefix("#") }
                            .filter { it.isNotEmpty() }

                        val updated = transaction.copy(
                            amount = amount,
                            type = selectedType.displayName,
                            accountId = accId,
                            toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                            categoryId = if (selectedType == TransactionType.TRANSFER) null else selectedCategoryId,
                            note = note.trim().ifEmpty { null },
                            labels = parsedLabels,
                            proofImageUri = proofImageUri
                        )

                        onSaveTransaction(updated)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("save_edited_tx_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showProofViewer && proofImageUri != null) {
        ProofImageViewerDialog(
            imageUri = proofImageUri!!,
            title = note.ifBlank { transaction.type },
            amountText = CurrencyUtils.formatCurrency(transaction.amount, baseCurrency),
            subtitle = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(transaction.date),
            onDismissRequest = { showProofViewer = false }
        )
    }
}
