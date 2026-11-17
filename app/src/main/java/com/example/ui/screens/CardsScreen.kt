package com.example.ui.screens

import androidx.compose.foundation.Image
import com.example.ui.brand.BrandMark
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import com.example.ui.brand.Brands
import com.example.ui.brand.rememberAppLogo
import com.example.ui.brand.BrandWordmark
import com.example.ui.brand.NetworkMark
import com.example.ui.brand.drawBrandPattern
import java.util.Locale
import android.content.ClipData
import android.content.ClipDescription
import android.content.ContextWrapper
import android.app.Activity
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.PaymentCardEntity
import com.example.util.BiometricAuthHelper
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardEvent
import com.example.viewmodels.DashboardViewModel

enum class CardFinishStyle(
    val id: String,
    val label: String,
    val colors: List<Color>,
    val accentColor: Color
) {
    OBSIDIAN_GOLD(
        "OBSIDIAN_GOLD",
        "Obsidian Gold",
        listOf(Color(0xFF141619), Color(0xFF262930), Color(0xFF3A301C)),
        Color(0xFFF59E0B)
    ),
    EMERALD_GLASS(
        "EMERALD_GLASS",
        "Emerald Executive",
        listOf(Color(0xFF042F2E), Color(0xFF065F46), Color(0xFF059669)),
        Color(0xFF34D399)
    ),
    ROYAL_SAPPHIRE(
        "ROYAL_SAPPHIRE",
        "Royal Sapphire",
        listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF2563EB)),
        Color(0xFF60A5FA)
    ),
    CRIMSON_TITANIUM(
        "CRIMSON_TITANIUM",
        "Crimson Titanium",
        listOf(Color(0xFF2A0815), Color(0xFF881337), Color(0xFFE11D48)),
        Color(0xFFFB7185)
    ),
    CYBER_VIOLET(
        "CYBER_VIOLET",
        "Cyber Violet",
        listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF7C3AED)),
        Color(0xFFC084FC)
    ),
    MATTE_CARBON(
        "MATTE_CARBON",
        "Stealth Carbon",
        listOf(Color(0xFF090A0F), Color(0xFF181B24), Color(0xFF272B38)),
        Color(0xFF94A3B8)
    ),
    BRONZE_LUXE(
        "BRONZE_LUXE",
        "Champagne Bronze",
        listOf(Color(0xFF27170E), Color(0xFF5C3419), Color(0xFF9A5B25)),
        Color(0xFFFBBF24)
    ),
    OCEAN_TEAL(
        "OCEAN_TEAL",
        "Pacific Cyan",
        listOf(Color(0xFF083344), Color(0xFF155E75), Color(0xFF0891B2)),
        Color(0xFF22D3EE)
    );

    companion object {
        fun fromId(id: String?): CardFinishStyle =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN_GOLD
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAccountLog: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val allCards by viewModel.allPaymentCards.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val hideMoney by viewModel.uiState.collectAsState()
    val requireBiometricForCards by viewModel.requireBiometricForCards.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.CardUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.TransactionLogged -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.Error -> snackbarHostState.showSnackbar(event.error)
                else -> Unit
            }
        }
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, DEBIT, CREDIT, VIRTUAL
    var revealedCardIds by remember { mutableStateOf(setOf<Long>()) }

    // While a full card number / CVV is on screen, block screenshots, screen recording and the Recents preview.
    val anyRevealed = revealedCardIds.isNotEmpty()
    DisposableEffect(anyRevealed) {
        val window = context.findActivity()?.window
        if (anyRevealed) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (anyRevealed) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    var showAddEditSheet by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<PaymentCardEntity?>(null) }
    var initialAddCardType by remember { mutableStateOf("DEBIT") }

    var selectedCardForDetails by remember { mutableStateOf<PaymentCardEntity?>(null) }
    var cardForLogSpend by remember { mutableStateOf<PaymentCardEntity?>(null) }
    var cardForPayBill by remember { mutableStateOf<PaymentCardEntity?>(null) }
    var cardForAdjustLimit by remember { mutableStateOf<PaymentCardEntity?>(null) }
    var cardToDelete by remember { mutableStateOf<PaymentCardEntity?>(null) }

    val accountMap by remember(allAccounts) {
        derivedStateOf { allAccounts.associateBy { it.id } }
    }

    val filteredCards by remember(allCards, selectedFilter) {
        derivedStateOf {
            when (selectedFilter) {
                "DEBIT" -> allCards.filter { !it.isCreditCard }
                "CREDIT" -> allCards.filter { it.isCreditCard }
                "VIRTUAL" -> allCards.filter { it.isVirtual || it.cardType.equals("VIRTUAL", ignoreCase = true) }
                else -> allCards
            }
        }
    }

    val creditCards by remember(allCards) {
        derivedStateOf { allCards.filter { it.isCreditCard } }
    }

    val debitCards by remember(allCards) {
        derivedStateOf { allCards.filter { !it.isCreditCard } }
    }

    val totalCreditLimit by remember(creditCards) {
        derivedStateOf { creditCards.sumOf { it.creditLimit } }
    }
    val totalCreditUsed by remember(creditCards) {
        derivedStateOf { creditCards.sumOf { it.currentCreditUsed } }
    }
    val totalAvailableCredit by remember(totalCreditLimit, totalCreditUsed) {
        derivedStateOf { (totalCreditLimit - totalCreditUsed).coerceAtLeast(0.0) }
    }

    fun formatAccountHierarchyName(accountId: Long?): String {
        if (accountId == null) return "Unlinked"
        val acc = accountMap[accountId] ?: return "Account #$accountId"
        val parent = acc.parentAccountId?.let { accountMap[it] }
        return if (parent != null) {
            "${parent.name} ▸ ${acc.name} (${acc.subAccountType ?: "Sub-Account"})"
        } else {
            "${acc.name} (${acc.type})"
        }
    }

    fun toggleRevealCard(cardId: Long) {
        if (revealedCardIds.contains(cardId)) {
            revealedCardIds = revealedCardIds - cardId
        } else {
            if (requireBiometricForCards) {
                BiometricAuthHelper.authenticate(
                    context = context,
                    title = "Reveal Sensitive Card Details",
                    subtitle = "Verify biometrics to view full card number, CVV & PIN",
                    onSuccess = {
                        revealedCardIds = revealedCardIds + cardId
                    },
                    onFallbackToPin = {
                        revealedCardIds = revealedCardIds + cardId
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                revealedCardIds = revealedCardIds + cardId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cards & Credit Vault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${debitCards.size} Debit/Linked • ${creditCards.size} Credit Cards",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("cards_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            cardToEdit = null
                            initialAddCardType = "CREDIT"
                            showAddEditSheet = true
                        },
                        modifier = Modifier.testTag("btn_top_add_credit_card")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    cardToEdit = null
                    initialAddCardType = if (selectedFilter == "CREDIT") "CREDIT" else "DEBIT"
                    showAddEditSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Card", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_payment_card")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Portfolio Credit Limit & Linked Debit Summary Header
            item(key = "CARDS_PORTFOLIO_SUMMARY") {
                CardsPortfolioOverviewCard(
                    creditCardCount = creditCards.size,
                    debitCardCount = debitCards.size,
                    totalCreditLimit = totalCreditLimit,
                    totalCreditUsed = totalCreditUsed,
                    totalAvailableCredit = totalAvailableCredit,
                    baseCurrency = baseCurrency,
                    hideMoney = hideMoney.hideMoney,
                    onQuickAddDebit = {
                        cardToEdit = null
                        initialAddCardType = "DEBIT"
                        showAddEditSheet = true
                    },
                    onQuickAddCredit = {
                        cardToEdit = null
                        initialAddCardType = "CREDIT"
                        showAddEditSheet = true
                    }
                )
            }

            // 2. Filter Chips Row
            item(key = "CARDS_FILTER_ROW") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf(
                        "ALL" to "All Cards (${allCards.size})",
                        "DEBIT" to "Debit & Linked (${debitCards.size})",
                        "CREDIT" to "Credit Limits (${creditCards.size})",
                        "VIRTUAL" to "Virtual (${allCards.count { it.isVirtual || it.cardType.equals("VIRTUAL", true) }})"
                    )
                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedFilter == key) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (selectedFilter == key) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("cards_filter_$key")
                        )
                    }
                }
            }

            // 3. Empty State or Cards List
            if (filteredCards.isEmpty()) {
                item(key = "CARDS_EMPTY_STATE") {
                    EmptyCardsVaultState(
                        onAddDebitClick = {
                            cardToEdit = null
                            initialAddCardType = "DEBIT"
                            showAddEditSheet = true
                        },
                        onAddCreditClick = {
                            cardToEdit = null
                            initialAddCardType = "CREDIT"
                            showAddEditSheet = true
                        }
                    )
                }
            } else {
                items(
                    items = filteredCards,
                    key = { "PAYMENT_CARD_${it.id}" }
                ) { card ->
                    val linkedAccount = card.linkedAccountId?.let { accountMap[it] }
                    val parentAccount = linkedAccount?.parentAccountId?.let { accountMap[it] }
                    val isRevealed = revealedCardIds.contains(card.id)

                    PaymentCardVaultItem(
                        card = card,
                        linkedAccount = linkedAccount,
                        parentAccount = parentAccount,
                        isRevealed = isRevealed,
                        hideMoney = hideMoney.hideMoney,
                        onToggleReveal = { toggleRevealCard(card.id) },
                        onOpenDetails = { selectedCardForDetails = card },
                        onEditCard = {
                            cardToEdit = card
                            showAddEditSheet = true
                        },
                        onLogSpend = { cardForLogSpend = card },
                        onPayCreditBill = { cardForPayBill = card },
                        onAdjustCreditLimit = { cardForAdjustLimit = card },
                        onOpenLinkedAccount = {
                            linkedAccount?.id?.let { onNavigateToAccountLog(it) }
                        },
                        onCopyText = { label, text ->
                            copyToClipboard(context, label, text)
                        }
                    )
                }
            }
        }
    }

    // --- Modals & Bottom Sheets ---

    if (showAddEditSheet) {
        AddEditPaymentCardSheet(
            existingCard = cardToEdit,
            initialCardType = initialAddCardType,
            accounts = allAccounts,
            defaultCurrency = baseCurrency,
            onDismiss = {
                showAddEditSheet = false
                cardToEdit = null
            },
            onSave = { savedCard ->
                viewModel.savePaymentCard(savedCard)
                showAddEditSheet = false
                cardToEdit = null
            }
        )
    }

    selectedCardForDetails?.let { card ->
        val liveCard = allCards.find { it.id == card.id } ?: card
        val linkedAcc = liveCard.linkedAccountId?.let { accountMap[it] }
        CardFullDetailsSheet(
            card = liveCard,
            linkedAccountName = formatAccountHierarchyName(liveCard.linkedAccountId),
            linkedAccountBalance = linkedAcc?.currentBalance,
            isRevealed = revealedCardIds.contains(liveCard.id),
            onToggleReveal = { toggleRevealCard(liveCard.id) },
            onToggleFreeze = { viewModel.toggleCardFrozen(liveCard) },
            onEdit = {
                selectedCardForDetails = null
                cardToEdit = liveCard
                showAddEditSheet = true
            },
            onDelete = {
                selectedCardForDetails = null
                cardToDelete = liveCard
            },
            onCopyText = { label, value -> copyToClipboard(context, label, value) },
            onDismiss = { selectedCardForDetails = null }
        )
    }

    cardForLogSpend?.let { card ->
        LogCardSpendDialog(
            card = card,
            linkedAccountName = formatAccountHierarchyName(card.linkedAccountId),
            accounts = allAccounts,
            categories = allCategories.filter { it.type.equals("Expense", ignoreCase = true) },
            onDismiss = { cardForLogSpend = null },
            onConfirm = { amount, categoryId, note, overrideAccId ->
                viewModel.logCardCharge(
                    card = card,
                    amount = amount,
                    categoryId = categoryId,
                    merchantOrNote = note,
                    overrideAccountId = overrideAccId
                )
                cardForLogSpend = null
            }
        )
    }

    cardForPayBill?.let { card ->
        PayCreditCardBillDialog(
            card = card,
            accounts = allAccounts,
            formatAccountHierarchy = ::formatAccountHierarchyName,
            onDismiss = { cardForPayBill = null },
            onConfirm = { payAmount, sourceAccountId, note ->
                viewModel.payCreditCardBill(
                    card = card,
                    paymentAmount = payAmount,
                    sourceAccountId = sourceAccountId,
                    note = note
                )
                cardForPayBill = null
            }
        )
    }

    cardForAdjustLimit?.let { card ->
        AdjustCreditLimitDialog(
            card = card,
            onDismiss = { cardForAdjustLimit = null },
            onConfirm = { newLimit, newUsed, newStatement ->
                viewModel.adjustCreditCardUsed(
                    card = card,
                    newUsedBalance = newUsed,
                    newStatementBalance = newStatement,
                    newCreditLimit = newLimit
                )
                cardForAdjustLimit = null
            }
        )
    }

    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Remove ${card.cardName}?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will remove ${card.cardName} (••${card.lastFourDigits}) from your Cards Vault. Existing ledger transactions in your accounts will remain untouched.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePaymentCard(card)
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CardsPortfolioOverviewCard(
    creditCardCount: Int,
    debitCardCount: Int,
    totalCreditLimit: Double,
    totalCreditUsed: Double,
    totalAvailableCredit: Double,
    baseCurrency: String,
    hideMoney: Boolean,
    onQuickAddDebit: () -> Unit,
    onQuickAddCredit: () -> Unit
) {
    val utilizationRatio = if (totalCreditLimit > 0.0) {
        (totalCreditUsed / totalCreditLimit).toFloat().coerceIn(0f, 1f)
    } else 0f
    val utilizationPct = (utilizationRatio * 100f).toInt()

    val statusColor = when {
        utilizationPct >= 75 -> Color(0xFFEF4444)
        utilizationPct >= 35 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Credit limit & utilization tracker",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (creditCardCount > 0) {
                                "Available Credit: ${CurrencyUtils.formatCurrency(totalAvailableCredit, baseCurrency, hideMoney)}"
                            } else {
                                "Track Credit Limits & Linked Debit Cards"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (totalCreditLimit > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$utilizationPct% Used",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (totalCreditLimit > 0.0) {
                LinearProgressIndicator(
                    progress = { utilizationRatio },
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Used",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(totalCreditUsed, baseCurrency, hideMoney),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Combined Credit Limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(totalCreditLimit, baseCurrency, hideMoney),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Add Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onQuickAddDebit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_add_debit_card")
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Debit Card", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onQuickAddCredit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_add_credit_card")
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Credit Card", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun PaymentCardVaultItem(
    card: PaymentCardEntity,
    linkedAccount: AccountEntity?,
    parentAccount: AccountEntity?,
    isRevealed: Boolean,
    hideMoney: Boolean,
    onToggleReveal: () -> Unit,
    onOpenDetails: () -> Unit,
    onEditCard: () -> Unit,
    onLogSpend: () -> Unit,
    onPayCreditBill: () -> Unit,
    onAdjustCreditLimit: () -> Unit,
    onOpenLinkedAccount: () -> Unit,
    onCopyText: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_card_item_${card.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Visual Physical/Virtual Card Face
            RealisticPaymentCardFace(
                card = card,
                isRevealed = isRevealed,
                onToggleReveal = onToggleReveal,
                onClick = onOpenDetails,
                onCopyNumber = {
                    if (card.cardNumber.isNotBlank()) {
                        onCopyText("Card Number", card.digitsOnlyNumber)
                    }
                },
                onCopyCvv = {
                    if (card.cvv.isNotBlank()) {
                        onCopyText("CVV", card.cvv)
                    }
                }
            )

            // 2. Connected Account/Sub-Account OR Credit Limit Telemetry Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (card.isCreditCard) {
                    // CREDIT CARD LIMIT TRACKER
                    val utilPct = card.utilizationPercent
                    val barColor = when {
                        card.isOverLimit || utilPct >= 80 -> Color(0xFFEF4444)
                        utilPct >= 35 -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CREDIT USED / LIMIT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = CurrencyUtils.formatCurrency(card.currentCreditUsed, card.currencyCode, hideMoney),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = barColor
                                )
                                Text(
                                    text = " / ${CurrencyUtils.formatCurrency(card.creditLimit, card.currencyCode, hideMoney)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "AVAILABLE CREDIT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(card.availableCredit, card.currencyCode, hideMoney),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { card.utilizationRatio },
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    // Billing Cycle & Statement Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoPillBadge(
                            text = "$utilPct% Utilization",
                            color = barColor
                        )
                        if (card.statementDay != null) {
                            InfoPillBadge(
                                text = "Stmt: Day ${card.statementDay}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (card.paymentDueDay != null) {
                            InfoPillBadge(
                                text = "Due: Day ${card.paymentDueDay}",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (card.statementBalance > 0.0) {
                            InfoPillBadge(
                                text = "Stmt Bal: ${CurrencyUtils.formatCurrency(card.statementBalance, card.currencyCode, hideMoney)}",
                                color = Color(0xFFF59E0B)
                            )
                        }
                        if (card.aprInterestRate > 0f) {
                            InfoPillBadge(
                                text = "${card.aprInterestRate}% APR",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Credit Card Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onLogSpend,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Charge", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPayCreditBill,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pay Bill", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onAdjustCreditLimit,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Adjust Credit Limit & Balance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenDetails,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "All Card Details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // DEBIT / VIRTUAL / PREPAID CARD (CONNECTED TO ACCOUNT OR SUB-ACCOUNT)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            0.8.dp,
                            if (linkedAccount != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (linkedAccount != null) Modifier.clickable { onOpenLinkedAccount() }
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (linkedAccount != null) Color(linkedAccount.color).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (linkedAccount?.isSubAccount == true) {
                                                Icons.Default.AccountTree
                                            } else {
                                                Icons.Default.AccountBalance
                                            },
                                            contentDescription = null,
                                            tint = if (linkedAccount != null) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (linkedAccount != null) {
                                            if (linkedAccount.isSubAccount && parentAccount != null) {
                                                "CONNECTED SUB-ACCOUNT (${linkedAccount.subAccountType ?: "Pocket"})"
                                            } else {
                                                "CONNECTED ${linkedAccount.type.uppercase()} ACCOUNT"
                                            }
                                        } else {
                                            "NO ACCOUNT LINKED"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (linkedAccount != null) {
                                            if (parentAccount != null) "${parentAccount.name} ▸ ${linkedAccount.name}"
                                            else linkedAccount.name
                                        } else {
                                            "Tap Edit to connect an Account or Sub-Account"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (linkedAccount != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Available Balance",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(
                                            linkedAccount.currentBalance,
                                            linkedAccount.currencyCode,
                                            hideMoney
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (linkedAccount.currentBalance < 0.0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (card.dailySpendLimit > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Card Spend / ATM Limit:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(card.dailySpendLimit, card.currencyCode, hideMoney),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Debit Card Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onLogSpend,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Log Debit Spend", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        if (linkedAccount != null) {
                            OutlinedButton(
                                onClick = onOpenLinkedAccount,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Account Log", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onEditCard,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Link Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onOpenDetails,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Full Card Details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealisticPaymentCardFace(
    card: PaymentCardEntity,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onClick: () -> Unit,
    onCopyNumber: (() -> Unit)? = null,
    onCopyCvv: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val finishStyle = remember(card.cardFinish) { CardFinishStyle.fromId(card.cardFinish) }
    // Recognised issuers (GCash, Maya, MariBank, GoTyme, BDO, BPI…) get their brand-styled design.
    val brand = remember(card.issuingBank, card.cardName) { Brands.detect(card.issuingBank, card.cardName) }
    val background = brand?.colors ?: finishStyle.colors
    val accent = brand?.accent ?: finishStyle.accentColor
    val ink = brand?.content ?: Color.White
    val inkMuted = ink.copy(alpha = 0.72f)
    val typeLabel = buildString {
        if (card.isVirtual) append("VIRTUAL ")
        append(card.cardType.uppercase(Locale.ROOT))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // ISO/IEC 7810 ID-1 card proportions
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = background))
            .drawBehind {
                if (brand != null) {
                    drawBrandPattern(brand.pattern, accent)
                } else {
                    drawCircle(accent.copy(alpha = 0.12f), radius = size.minDimension * 0.7f, center = Offset(size.width * 0.95f, size.height * 0.1f))
                }
                // soft top sheen
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), endY = size.height * 0.45f))
            }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Issuer wordmark + card type
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                val appLogo = rememberAppLogo(card.issuingBank.ifBlank { card.cardName })
                if (appLogo != null) {
                    Image(
                        bitmap = appLogo,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (brand != null) {
                        BrandWordmark(brand, fontSize = 22.sp)
                    } else {
                        Text(
                            text = card.issuingBank.ifBlank { card.cardName }.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ink,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val shownTitle = brand?.name ?: card.issuingBank.ifBlank { card.cardName }
                    if (!card.cardName.trim().equals(shownTitle.trim(), ignoreCase = true)) {
                        Text(
                            text = card.cardName,
                            style = MaterialTheme.typography.labelMedium,
                            color = inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(typeLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = inkMuted, letterSpacing = 1.sp)
                    if (card.isFrozen) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0EA5E9).copy(alpha = 0.28f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                Text("Frozen", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Chip + contactless, then the number with reveal / copy
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Canvas(modifier = Modifier.size(width = 36.dp, height = 27.dp)) {
                        val r = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFF3DD9B), Color(0xFFC9A04A), Color(0xFFE9CF85))), cornerRadius = r)
                        val line = Color(0xFF6B5221).copy(alpha = 0.45f)
                        val sw = 1.dp.toPx()
                        drawLine(line, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), sw)
                        drawLine(line, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), sw)
                        drawLine(line, Offset(size.width * 0.67f, 0f), Offset(size.width * 0.67f, size.height), sw)
                        drawRoundRect(line, cornerRadius = r, style = Stroke(sw))
                    }
                    Icon(Icons.Rounded.Contactless, contentDescription = "Contactless", tint = inkMuted, modifier = Modifier.size(22.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRevealed) card.formattedCardNumber else card.maskedCardNumber,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (isRevealed && card.cardNumber.isNotBlank() && onCopyNumber != null) {
                        IconButton(onClick = onCopyNumber, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy card number", tint = ink, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onToggleReveal, modifier = Modifier.size(32.dp).testTag("btn_reveal_card_${card.id}")) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (isRevealed) "Hide card details" else "Show card details",
                            tint = ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Cardholder, expiry, CVV (only when revealed) and network mark
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.cardholderName.ifBlank { "CARDHOLDER" }.uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ink,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("EXP ${card.formattedExpiry}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = inkMuted)
                        if (isRevealed) {
                            Text(
                                text = "CVV ${card.cvv.ifBlank { "---" }}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = ink,
                                modifier = if (onCopyCvv != null) Modifier.clickable { onCopyCvv() } else Modifier
                            )
                        }
                    }
                }
                NetworkMark(card.cardNetwork, onDark = brand?.id != "wise", height = 26.dp)
            }
        }
    }
}

@Composable
private fun InfoPillBadge(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyCardsVaultState(
    onAddDebitClick: () -> Unit,
    onAddCreditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = "Your Cards Vault is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Save your Debit Cards (connected directly to any Account or Sub-Account) and Credit Cards (with automatic credit limit & utilization tracking, billing cycle dates, and full card details).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                OutlinedButton(onClick = onAddDebitClick, shape = RoundedCornerShape(12.dp)) {
                    Text("Add Debit Card", fontWeight = FontWeight.Bold)
                }
                Button(onClick = onAddCreditClick, shape = RoundedCornerShape(12.dp)) {
                    Text("Add Credit Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardFullDetailsSheet(
    card: PaymentCardEntity,
    linkedAccountName: String,
    linkedAccountBalance: Double?,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onToggleFreeze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RealisticPaymentCardFace(
                card = card,
                isRevealed = isRevealed,
                onToggleReveal = onToggleReveal,
                onClick = onToggleReveal,
                onCopyNumber = {
                    if (card.cardNumber.isNotBlank()) {
                        onCopyText("Card Number", card.digitsOnlyNumber)
                    }
                },
                onCopyCvv = {
                    if (card.cvv.isNotBlank()) {
                        onCopyText("CVV", card.cvv)
                    }
                }
            )

            // Quick Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleReveal,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRevealed) "Mask Details" else "Reveal All Details", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Card", fontWeight = FontWeight.Bold)
                }
            }

            // All Card Details Vault Table
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "COMPLETE CARD DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    DetailCopyRow(
                        label = "Card Nickname",
                        value = card.cardName,
                        onCopy = { onCopyText("Card Name", card.cardName) }
                    )
                    DetailCopyRow(
                        label = "Card Type & Network",
                        value = "${if (card.isVirtual) "Virtual " else ""}${card.cardType} • ${card.cardNetwork}"
                    )
                    if (card.issuingBank.isNotBlank()) {
                        DetailCopyRow(
                            label = "Issuing Bank / Institution",
                            value = card.issuingBank
                        )
                    }
                    DetailCopyRow(
                        label = "Cardholder Name",
                        value = card.cardholderName.ifBlank { "Not specified" },
                        onCopy = if (card.cardholderName.isNotBlank()) {
                            { onCopyText("Cardholder Name", card.cardholderName) }
                        } else null
                    )
                    DetailCopyRow(
                        label = "Card Number",
                        value = if (isRevealed) card.formattedCardNumber else card.maskedCardNumber,
                        onCopy = if (card.cardNumber.isNotBlank()) {
                            { onCopyText("Card Number", card.digitsOnlyNumber) }
                        } else null
                    )
                    DetailCopyRow(
                        label = "Expiration Date (MM/YY)",
                        value = card.formattedExpiry,
                        onCopy = { onCopyText("Expiry Date", card.formattedExpiry) }
                    )
                    DetailCopyRow(
                        label = "CVV / Security Code",
                        value = if (isRevealed) card.cvv.ifBlank { "Not set" } else "••• (Tap Reveal)",
                        onCopy = if (isRevealed && card.cvv.isNotBlank()) {
                            { onCopyText("CVV", card.cvv) }
                        } else null
                    )
                    if (!card.cardPinHint.isNullOrBlank()) {
                        DetailCopyRow(
                            label = "ATM / App PIN or Hint",
                            value = if (isRevealed) card.cardPinHint else "•••• (Tap Reveal)",
                            onCopy = if (isRevealed) {
                                { onCopyText("Card PIN", card.cardPinHint) }
                            } else null
                        )
                    }
                    if (!card.billingAddress.isNullOrBlank()) {
                        DetailCopyRow(
                            label = "Billing Address / ZIP",
                            value = card.billingAddress,
                            onCopy = { onCopyText("Billing Address", card.billingAddress) }
                        )
                    }
                    if (!card.customerServicePhone.isNullOrBlank()) {
                        DetailCopyRow(
                            label = "Bank Hotline / Emergency Support",
                            value = card.customerServicePhone,
                            onCopy = { onCopyText("Bank Hotline", card.customerServicePhone) }
                        )
                    }
                    if (!card.rewardsProgram.isNullOrBlank()) {
                        DetailCopyRow(
                            label = "Rewards / Cashback Program",
                            value = card.rewardsProgram
                        )
                    }
                    if (!card.notes.isNullOrBlank()) {
                        DetailCopyRow(
                            label = "Card Notes",
                            value = card.notes
                        )
                    }
                }
            }

            // Financial Connection or Credit Limit Breakdown
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (card.isCreditCard) {
                        Text(
                            text = "CREDIT LIMIT & BILLING CYCLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        DetailCopyRow(
                            label = "Total Credit Limit",
                            value = CurrencyUtils.formatCurrency(card.creditLimit, card.currencyCode)
                        )
                        DetailCopyRow(
                            label = "Current Credit Used",
                            value = "${CurrencyUtils.formatCurrency(card.currentCreditUsed, card.currencyCode)} (${card.utilizationPercent}%)"
                        )
                        DetailCopyRow(
                            label = "Available Credit",
                            value = CurrencyUtils.formatCurrency(card.availableCredit, card.currencyCode)
                        )
                        DetailCopyRow(
                            label = "Statement Balance",
                            value = CurrencyUtils.formatCurrency(card.statementBalance, card.currencyCode)
                        )
                        DetailCopyRow(
                            label = "Minimum Payment Due",
                            value = CurrencyUtils.formatCurrency(card.minimumPaymentDue, card.currencyCode)
                        )
                        DetailCopyRow(
                            label = "Statement Cutoff / Due Day",
                            value = "Cutoff: ${card.statementDay?.let { "Every ${it}th" } ?: "Not set"} • Due: ${card.paymentDueDay?.let { "Every ${it}th" } ?: "Not set"}"
                        )
                        DetailCopyRow(
                            label = "Interest Rate (APR) & Annual Fee",
                            value = "${card.aprInterestRate}% APR • Fee: ${CurrencyUtils.formatCurrency(card.annualFee, card.currencyCode)}"
                        )
                    } else {
                        Text(
                            text = "CONNECTED ACCOUNT / SUB-ACCOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        DetailCopyRow(
                            label = "Linked Account / Sub-Account",
                            value = linkedAccountName
                        )
                        if (linkedAccountBalance != null) {
                            DetailCopyRow(
                                label = "Live Connected Balance",
                                value = CurrencyUtils.formatCurrency(linkedAccountBalance, card.currencyCode)
                            )
                        }
                        if (card.dailySpendLimit > 0.0) {
                            DetailCopyRow(
                                label = "Daily Spending / ATM Limit",
                                value = CurrencyUtils.formatCurrency(card.dailySpendLimit, card.currencyCode)
                            )
                        }
                    }
                }
            }

            // Freeze Switch & Delete Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Freeze Card", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Prevent accidental charge logging while frozen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = card.isFrozen,
                    onCheckedChange = { onToggleFreeze() }
                )
            }

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Card from Vault", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailCopyRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPaymentCardSheet(
    existingCard: PaymentCardEntity?,
    initialCardType: String,
    accounts: List<AccountEntity>,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (PaymentCardEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var cardType by remember { mutableStateOf(existingCard?.cardType ?: initialCardType) }
    var cardName by remember { mutableStateOf(existingCard?.cardName ?: "") }
    var issuingBank by remember { mutableStateOf(existingCard?.issuingBank ?: "") }
    var cardNetwork by remember { mutableStateOf(existingCard?.cardNetwork ?: "VISA") }
    var cardholderName by remember { mutableStateOf(existingCard?.cardholderName ?: "") }
    var cardNumber by remember { mutableStateOf(existingCard?.cardNumber ?: "") }
    var expiryMonth by remember { mutableStateOf(existingCard?.expiryMonth?.toString() ?: "12") }
    var expiryYear by remember { mutableStateOf(existingCard?.expiryYear?.toString() ?: "28") }
    var cvv by remember { mutableStateOf(existingCard?.cvv ?: "") }
    var cardPinHint by remember { mutableStateOf(existingCard?.cardPinHint ?: "") }
    var billingAddress by remember { mutableStateOf(existingCard?.billingAddress ?: "") }
    var customerServicePhone by remember { mutableStateOf(existingCard?.customerServicePhone ?: "") }
    var currencyCode by remember { mutableStateOf(existingCard?.currencyCode ?: defaultCurrency) }
    var cardFinish by remember {
        mutableStateOf(
            existingCard?.cardFinish
                ?: if (initialCardType == "CREDIT") CardFinishStyle.OBSIDIAN_GOLD.id else CardFinishStyle.EMERALD_GLASS.id
        )
    }
    var isVirtual by remember {
        mutableStateOf(existingCard?.isVirtual ?: (initialCardType == "VIRTUAL"))
    }

    // Debit / Linked Account or Sub-Account
    var linkedAccountId by remember {
        mutableStateOf<Long?>(existingCard?.linkedAccountId ?: accounts.firstOrNull()?.id)
    }
    var dailySpendLimit by remember {
        mutableStateOf(
            if ((existingCard?.dailySpendLimit ?: 0.0) > 0.0) existingCard!!.dailySpendLimit.toString() else ""
        )
    }

    // Credit Card Limit & Billing Fields
    var creditLimit by remember {
        mutableStateOf(
            if ((existingCard?.creditLimit ?: 0.0) > 0.0) existingCard!!.creditLimit.toString() else ""
        )
    }
    var currentCreditUsed by remember {
        mutableStateOf(
            if ((existingCard?.currentCreditUsed ?: 0.0) > 0.0) existingCard!!.currentCreditUsed.toString() else "0"
        )
    }
    var statementBalance by remember {
        mutableStateOf(
            if ((existingCard?.statementBalance ?: 0.0) > 0.0) existingCard!!.statementBalance.toString() else ""
        )
    }
    var minimumPaymentDue by remember {
        mutableStateOf(
            if ((existingCard?.minimumPaymentDue ?: 0.0) > 0.0) existingCard!!.minimumPaymentDue.toString() else ""
        )
    }
    var statementDay by remember { mutableStateOf(existingCard?.statementDay?.toString() ?: "") }
    var paymentDueDay by remember { mutableStateOf(existingCard?.paymentDueDay?.toString() ?: "") }
    var aprInterestRate by remember {
        mutableStateOf(
            if ((existingCard?.aprInterestRate ?: 0f) > 0f) existingCard!!.aprInterestRate.toString() else ""
        )
    }
    var annualFee by remember {
        mutableStateOf(
            if ((existingCard?.annualFee ?: 0.0) > 0.0) existingCard!!.annualFee.toString() else ""
        )
    }
    var rewardsProgram by remember { mutableStateOf(existingCard?.rewardsProgram ?: "") }
    var notes by remember { mutableStateOf(existingCard?.notes ?: "") }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    // Organize accounts so Main Accounts and their Sub-Accounts are easy to pick
    val orderedAccounts = remember(accounts) {
        val mainAccs = accounts.filter { it.parentAccountId == null }
        val subByParent = accounts.filter { it.parentAccountId != null }.groupBy { it.parentAccountId }
        buildList {
            mainAccs.forEach { main ->
                add(main)
                subByParent[main.id]?.forEach { sub -> add(sub) }
            }
            // Any orphaned sub-accounts
            accounts.filter { it.parentAccountId != null && !accountMap.containsKey(it.parentAccountId) }
                .forEach { add(it) }
        }
    }

    fun formatAccountOptionLabel(acc: AccountEntity): String {
        val parent = acc.parentAccountId?.let { accountMap[it] }
        val bal = CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)
        return if (parent != null) {
            "↳ ${parent.name} ▸ ${acc.name} (${acc.subAccountType ?: "Sub-Account"}) • $bal"
        } else {
            "${acc.name} (${acc.type}) • $bal"
        }
    }

    val previewCard = PaymentCardEntity(
        id = existingCard?.id ?: 0L,
        cardName = cardName.ifBlank { if (cardType == "CREDIT") "Platinum Credit" else "Primary Debit" },
        cardType = cardType,
        cardNetwork = cardNetwork,
        issuingBank = issuingBank.ifBlank { "MY BANK" },
        cardholderName = cardholderName.ifBlank { "YOUR NAME" },
        cardNumber = cardNumber,
        expiryMonth = expiryMonth.toIntOrNull()?.coerceIn(1, 12) ?: 12,
        expiryYear = expiryYear.toIntOrNull() ?: 28,
        cvv = cvv.ifBlank { "123" },
        currencyCode = currencyCode,
        cardFinish = cardFinish,
        isVirtual = isVirtual
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingCard == null) "Save New Card to Vault" else "Edit ${existingCard.cardName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            // Live Interactive Card Preview
            RealisticPaymentCardFace(
                card = previewCard,
                isRevealed = true,
                onToggleReveal = {},
                onClick = {}
            )

            // 1. Card Type Selector (Debit, Credit, Virtual, Prepaid)
            Text(
                text = "1. CARD TYPE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("DEBIT" to "Debit Card", "CREDIT" to "Credit Card", "VIRTUAL" to "Virtual", "PREPAID" to "Prepaid").forEach { (typeKey, label) ->
                    val selected = cardType.equals(typeKey, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            cardType = typeKey
                            if (typeKey == "VIRTUAL") isVirtual = true
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("chip_card_type_$typeKey")
                    )
                }
            }

            // 2. Card Finish Theme Selector
            Text(
                text = "2. CARD FINISH & SKIN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            val detectedBrand = remember(issuingBank, cardName) { Brands.detect(issuingBank, cardName) }
            if (detectedBrand != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark(detectedBrand, size = 32.dp)
                    Text(
                        text = "${detectedBrand.name} card design applied automatically. The finish below is used if you change the issuer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardFinishStyle.entries.forEach { style ->
                    val isSelected = cardFinish == style.id
                    Surface(
                        onClick = { cardFinish = style.id },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 0.8.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Brush.linearGradient(style.colors))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Account/Sub-Account Connection (Debit/Prepaid/Virtual) OR Credit Limit Setup (Credit Card)
            if (cardType.equals("CREDIT", ignoreCase = true)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "3. CREDIT LIMIT & BILLING TRACKER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = creditLimit,
                                onValueChange = { creditLimit = it },
                                label = { Text("Credit Limit *") },
                                placeholder = { Text("e.g. 100000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_card_credit_limit")
                            )
                            OutlinedTextField(
                                value = currentCreditUsed,
                                onValueChange = { currentCreditUsed = it },
                                label = { Text("Current Used") },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_card_credit_used")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = statementBalance,
                                onValueChange = { statementBalance = it },
                                label = { Text("Statement Balance") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minimumPaymentDue,
                                onValueChange = { minimumPaymentDue = it },
                                label = { Text("Minimum Due") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = statementDay,
                                onValueChange = { statementDay = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Statement Day (1-31)") },
                                placeholder = { Text("15") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = paymentDueDay,
                                onValueChange = { paymentDueDay = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Due Day (1-31)") },
                                placeholder = { Text("5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = aprInterestRate,
                                onValueChange = { aprInterestRate = it },
                                label = { Text("Interest / APR %") },
                                placeholder = { Text("3.0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = annualFee,
                                onValueChange = { annualFee = it },
                                label = { Text("Annual Fee") },
                                placeholder = { Text("2500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "3. CONNECT TO ACCOUNT OR SUB-ACCOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select the Main Account or Sub-Account (Savings Pocket, Wallet, Stash) that funds this Debit Card:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ExposedDropdownMenuBox(
                            expanded = accountDropdownExpanded,
                            onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                        ) {
                            val selectedAcc = linkedAccountId?.let { accountMap[it] }
                            OutlinedTextField(
                                value = if (selectedAcc != null) formatAccountOptionLabel(selectedAcc) else "No Account Linked",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Connected Account / Sub-Account") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("dropdown_card_linked_account")
                            )
                            ExposedDropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (Standalone Card)") },
                                    onClick = {
                                        linkedAccountId = null
                                        accountDropdownExpanded = false
                                    }
                                )
                                orderedAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = formatAccountOptionLabel(acc),
                                                fontWeight = if (acc.isSubAccount) FontWeight.Medium else FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            linkedAccountId = acc.id
                                            currencyCode = acc.currencyCode
                                            if (issuingBank.isBlank()) {
                                                val parent = acc.parentAccountId?.let { accountMap[it] }
                                                issuingBank = parent?.name ?: acc.name
                                            }
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dailySpendLimit,
                            onValueChange = { dailySpendLimit = it },
                            label = { Text("Daily Spending / ATM Limit (Optional)") },
                            placeholder = { Text("e.g. 50000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. All Card Details (Name, Bank, Network, Card Number, Expiry, CVV, Holder, PIN, Billing)
            Text(
                text = "4. CARD CREDENTIALS & ALL DETAILS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card Name / Label *") },
                    placeholder = { Text("e.g. BPI Blue / MariBank Debit") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("input_card_name")
                )
                OutlinedTextField(
                    value = issuingBank,
                    onValueChange = { issuingBank = it },
                    label = { Text("Bank / Issuer") },
                    placeholder = { Text("BPI, Maya, BDO") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_card_bank")
                )
            }

            // Card Network Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("VISA", "MASTERCARD", "AMEX", "JCB", "UNIONPAY", "OTHER").forEach { net ->
                    FilterChip(
                        selected = cardNetwork.equals(net, ignoreCase = true),
                        onClick = { cardNetwork = net },
                        label = { Text(net, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            OutlinedTextField(
                value = cardholderName,
                onValueChange = { cardholderName = it },
                label = { Text("Cardholder Name (as embossed on card)") },
                placeholder = { Text("JUAN DELA CRUZ") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_cardholder_name")
            )

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { input ->
                    cardNumber = input.filter { it.isDigit() || it == ' ' }.take(23)
                },
                label = { Text("Card Number (16 Digits or Last 4 Digits)") },
                placeholder = { Text("4532 0123 4567 8901") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_card_number")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = expiryMonth,
                    onValueChange = { expiryMonth = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Exp Month (MM)") },
                    placeholder = { Text("08") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_card_exp_month")
                )
                OutlinedTextField(
                    value = expiryYear,
                    onValueChange = { expiryYear = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Exp Year (YY)") },
                    placeholder = { Text("29") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_card_exp_year")
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("CVV / CVC") },
                    placeholder = { Text("123") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_card_cvv")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cardPinHint,
                    onValueChange = { cardPinHint = it },
                    label = { Text("ATM PIN / Security Hint") },
                    placeholder = { Text("Masked until revealed") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it.uppercase().take(3) },
                    label = { Text("Currency") },
                    placeholder = { Text("PHP") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f)
                )
            }

            OutlinedTextField(
                value = billingAddress,
                onValueChange = { billingAddress = it },
                label = { Text("Billing Address / Postal ZIP Code") },
                placeholder = { Text("For online checkout reference") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customerServicePhone,
                    onValueChange = { customerServicePhone = it },
                    label = { Text("Bank Hotline") },
                    placeholder = { Text("+63 2 889 10000") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rewardsProgram,
                    onValueChange = { rewardsProgram = it },
                    label = { Text("Rewards / Cashback") },
                    placeholder = { Text("1.5% Cashback") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Card Perks & Additional Notes") },
                placeholder = { Text("Waived annual fee, lounge access, etc.") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val finalName = cardName.trim().ifBlank {
                        if (cardType.equals("CREDIT", true)) "${issuingBank.ifBlank { cardNetwork }} Credit Card"
                        else "${issuingBank.ifBlank { cardNetwork }} Debit Card"
                    }
                    val entity = PaymentCardEntity(
                        id = existingCard?.id ?: 0L,
                        cardName = finalName,
                        cardType = cardType.uppercase(),
                        cardNetwork = cardNetwork.uppercase(),
                        issuingBank = issuingBank.trim(),
                        cardholderName = cardholderName.trim(),
                        cardNumber = cardNumber.trim(),
                        expiryMonth = (expiryMonth.toIntOrNull() ?: 12).coerceIn(1, 12),
                        expiryYear = (expiryYear.toIntOrNull() ?: 28) % 100,
                        cvv = cvv.trim(),
                        cardPinHint = cardPinHint.trim().takeIf { it.isNotBlank() },
                        billingAddress = billingAddress.trim().takeIf { it.isNotBlank() },
                        customerServicePhone = customerServicePhone.trim().takeIf { it.isNotBlank() },
                        currencyCode = currencyCode.trim().ifBlank { defaultCurrency },
                        cardFinish = cardFinish,
                        isVirtual = isVirtual || cardType.equals("VIRTUAL", true),
                        isFrozen = existingCard?.isFrozen ?: false,
                        linkedAccountId = if (cardType.equals("CREDIT", true)) null else linkedAccountId,
                        dailySpendLimit = dailySpendLimit.toDoubleOrNull() ?: 0.0,
                        creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                        currentCreditUsed = currentCreditUsed.toDoubleOrNull() ?: 0.0,
                        statementBalance = statementBalance.toDoubleOrNull() ?: 0.0,
                        minimumPaymentDue = minimumPaymentDue.toDoubleOrNull() ?: 0.0,
                        statementDay = statementDay.toIntOrNull()?.coerceIn(1, 31),
                        paymentDueDay = paymentDueDay.toIntOrNull()?.coerceIn(1, 31),
                        aprInterestRate = aprInterestRate.toFloatOrNull() ?: 0f,
                        annualFee = annualFee.toDoubleOrNull() ?: 0.0,
                        rewardsProgram = rewardsProgram.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() },
                        createdAt = existingCard?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(entity)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_payment_card")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingCard == null) "Save Card to Vault" else "Update Card Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogCardSpendDialog(
    card: PaymentCardEntity,
    linkedAccountName: String,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long?, String?, Long?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantNote by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(categories.firstOrNull()?.id) }
    var fallbackAccountId by remember {
        mutableStateOf<Long?>(card.linkedAccountId ?: accounts.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (card.isCreditCard) "Log Credit Charge • ${card.cardName}"
                else "Log Debit Spend • ${card.cardName}",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (card.isCreditCard) {
                    Text(
                        text = "Available Credit: ${CurrencyUtils.formatCurrency(card.availableCredit, card.currencyCode)} (Limit: ${CurrencyUtils.formatCurrency(card.creditLimit, card.currencyCode)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Deducts from: $linkedAccountName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Purchase Amount (${card.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchantNote,
                    onValueChange = { merchantNote = it },
                    label = { Text("Merchant / Description") },
                    placeholder = { Text("e.g. Grab, Amazon, Grocery") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(
                            amt,
                            selectedCategoryId,
                            merchantNote.takeIf { it.isNotBlank() },
                            if (card.isCreditCard) null else fallbackAccountId
                        )
                    }
                }
            ) {
                Text("Log Charge", fontWeight = FontWeight.Bold)
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
private fun PayCreditCardBillDialog(
    card: PaymentCardEntity,
    accounts: List<AccountEntity>,
    formatAccountHierarchy: (Long?) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long, String?) -> Unit
) {
    var payAmountText by remember {
        mutableStateOf(
            if (card.statementBalance > 0.0) card.statementBalance.toString()
            else if (card.currentCreditUsed > 0.0) card.currentCreditUsed.toString()
            else ""
        )
    }
    var selectedSourceAccountId by remember {
        mutableStateOf(accounts.firstOrNull()?.id ?: 0L)
    }
    var noteText by remember { mutableStateOf("") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pay Credit Card Bill • ${card.cardName}", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current Used: ${CurrencyUtils.formatCurrency(card.currentCreditUsed, card.currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (card.currentCreditUsed > 0.0) {
                        FilterChip(
                            selected = payAmountText.toDoubleOrNull() == card.currentCreditUsed,
                            onClick = { payAmountText = card.currentCreditUsed.toString() },
                            label = { Text("Full Used", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (card.statementBalance > 0.0) {
                        FilterChip(
                            selected = payAmountText.toDoubleOrNull() == card.statementBalance,
                            onClick = { payAmountText = card.statementBalance.toString() },
                            label = { Text("Statement", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (card.minimumPaymentDue > 0.0) {
                        FilterChip(
                            selected = payAmountText.toDoubleOrNull() == card.minimumPaymentDue,
                            onClick = { payAmountText = card.minimumPaymentDue.toString() },
                            label = { Text("Min Due", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = payAmountText,
                    onValueChange = { payAmountText = it },
                    label = { Text("Payment Amount (${card.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = formatAccountHierarchy(selectedSourceAccountId),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pay From Account / Sub-Account") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Text("${formatAccountHierarchy(acc.id)} (${CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode)})")
                                },
                                onClick = {
                                    selectedSourceAccountId = acc.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Payment Reference / Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = payAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && selectedSourceAccountId > 0L) {
                        onConfirm(amt, selectedSourceAccountId, noteText.takeIf { it.isNotBlank() })
                    }
                }
            ) {
                Text("Pay & Restore Limit", fontWeight = FontWeight.Bold)
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
private fun AdjustCreditLimitDialog(
    card: PaymentCardEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double) -> Unit
) {
    var limitText by remember { mutableStateOf(card.creditLimit.toString()) }
    var usedText by remember { mutableStateOf(card.currentCreditUsed.toString()) }
    var statementText by remember { mutableStateOf(card.statementBalance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Calibrate Credit Limit • ${card.cardName}", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Total Credit Limit (${card.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = usedText,
                    onValueChange = { usedText = it },
                    label = { Text("Current Credit Used (${card.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = statementText,
                    onValueChange = { statementText = it },
                    label = { Text("Statement Balance Due (${card.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        limitText.toDoubleOrNull() ?: card.creditLimit,
                        usedText.toDoubleOrNull() ?: card.currentCreditUsed,
                        statementText.toDoubleOrNull() ?: card.statementBalance
                    )
                }
            ) {
                Text("Save Limits", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Card details are copied as sensitive (hidden from the keyboard's clipboard preview on Android 13+) and are
 * cleared from the clipboard after a minute if nothing else was copied since.
 */
private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText(label, value)
    clip.description.extras = PersistableBundle().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        else putBoolean("android.content.extra.IS_SENSITIVE", true)
    }
    clipboard.setPrimaryClip(clip)
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        runCatching {
            val current = clipboard.primaryClip
            if (current != null && current.itemCount > 0 && current.getItemAt(0).text?.toString() == value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }, 60_000L)
    Toast.makeText(context, "$label copied · clears in 1 minute", Toast.LENGTH_SHORT).show()
}
