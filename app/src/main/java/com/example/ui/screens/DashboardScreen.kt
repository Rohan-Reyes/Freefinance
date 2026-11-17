package com.example.ui.screens

import androidx.compose.material.icons.rounded.LocalCafe
import com.example.ui.theme.FinNegative
import com.example.ui.theme.FinPositive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.SmallFloatingActionButton
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.PlannedPayoutEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.DashboardCardType
import com.example.data.local.model.TransactionType
import com.example.data.preferences.HeroBackgroundStyle
import com.example.ui.components.AddAccountSheet
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.CashFlowForecastChart
import com.example.ui.components.EmptyListPlaceholder
import com.example.ui.components.modular.BudgetPercentCard
import com.example.ui.components.modular.CardDetailModalSheet
import com.example.ui.components.modular.DebtSummaryCard
import com.example.ui.components.modular.LiquidVsLockedCard
import com.example.ui.components.modular.LongTermExpectedCard
import com.example.ui.components.modular.MoneyFlowCard
import com.example.ui.components.modular.MonthlySpendingTrendsCard
import com.example.ui.components.modular.PlannedPayoutsCard
import com.example.ui.components.modular.ProjectTrackerCard
import com.example.ui.components.modular.SavingsRateCard
import com.example.ui.components.modular.SpendingCategoriesCard
import com.example.ui.components.modular.UpcomingBillsCard
import com.example.ui.components.modular.WeeklyForecastCard
import com.example.util.BankAppLauncher
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardEvent
import com.example.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAccountLog: (Long) -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    onNavigateToCustomize: () -> Unit = {},
    onNavigateToCommission: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    onNavigateToWidgets: () -> Unit = {},
    onNavigateToReceiptInbox: () -> Unit = {},
    onNavigateToAssets: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToCards: () -> Unit = {},
    initialOpenQuickAdd: Boolean = false,
    onQuickAddHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val activeCards by viewModel.activeCards.collectAsState()
    val cardOrder by viewModel.cardOrder.collectAsState()
    val forecastState by viewModel.forecastState.collectAsState()
    val weeklyForecastState by viewModel.weeklyForecastState.collectAsState()
    val budgetState by viewModel.budgetState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val moneyFlowState by viewModel.moneyFlowState.collectAsState()
    val liquidVsLockedState by viewModel.liquidVsLockedState.collectAsState()
    val projectTrackerState by viewModel.projectTrackerState.collectAsState()
    val longTermExpectedState by viewModel.longTermExpectedState.collectAsState()
    val topReceivables by viewModel.topReceivables.collectAsState()
    val totalActiveLent by viewModel.totalActiveLent.collectAsState()
    val heroBackgroundStyle by viewModel.heroBackgroundStyle.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val monthlySpendingTrends by viewModel.monthlySpendingTrends.collectAsState()
    val pendingReceiptCount by viewModel.pendingReceiptCount.collectAsState()
    val plannedPayouts by viewModel.allPlannedPayouts.collectAsState()
    val paymentCards by viewModel.allPaymentCards.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val hideMoney by viewModel.hideMoney.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()

    var selectedCardDetail by remember { mutableStateOf<DashboardCardType?>(null) }
    var showAddTransactionSheet by remember { mutableStateOf(initialOpenQuickAdd) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var targetParentAccountIdForSubAccount by remember { mutableStateOf<Long?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialOpenQuickAdd) {
        if (initialOpenQuickAdd) {
            showAddTransactionSheet = true
            onQuickAddHandled()
        }
    }

    // JSON Export / Import Launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportStateToJson(context, uri)
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importStateFromJson(context, uri)
        }
    }

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
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is DashboardEvent.DebtUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.AccountCreated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.ProjectUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.BudgetUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.SubscriptionUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.CurrencyUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.ReceiptSnapped -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.ReceiptConverted -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.ReceiptDiscarded -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.AssetUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.PlannedPayoutUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.CardUpdated -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.SyncCompleted -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.ExportCompleted -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.Error -> snackbarHostState.showSnackbar(event.error)
            }
        }
    }

    val dashboardListStateForFab = rememberLazyListState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "My Finance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("dashboard_hamburger_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cards & Credit Vault (${paymentCards.size})") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToCards()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings & Themes") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lock Vault (Biometrics)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (!biometricLockEnabled) {
                                        viewModel.setBiometricLockEnabled(true)
                                    }
                                    viewModel.lockAppNow()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Analytics & Forecast") },
                                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAnalytics()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Receipts Inbox & Review")
                                        if (pendingReceiptCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.error
                                            ) {
                                                Text(
                                                    text = "$pendingReceiptCount",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToReceiptInbox()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Category Manager") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToCategories()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Assets & Valuables") },
                                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAssets()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Customize Dashboard") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToCustomize()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Home Screen Widgets") },
                                leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToWidgets()
                                }
                            )
                        }
                    }
                },
                actions = {
                    if (canUndo) {
                        IconButton(
                            onClick = { viewModel.undoLastTransaction() },
                            modifier = Modifier.testTag("dashboard_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Transaction",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleHideMoney(!hideMoney) },
                        modifier = Modifier.testTag("dashboard_toggle_hide_money")
                    ) {
                        Icon(
                            imageVector = if (hideMoney) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideMoney) "Show Balances" else "Hide Balances",
                            tint = if (hideMoney) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onNavigateToReceiptInbox,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("dashboard_fab_quick_pic")
                ) {
                    Icon(Icons.Rounded.DocumentScanner, contentDescription = "Snap a receipt")
                }
                ExtendedFloatingActionButton(
                    onClick = { showAddTransactionSheet = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add") },
                    expanded = !dashboardListStateForFab.isScrollInProgress,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("dashboard_fab_log_entry")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val dashboardListState = dashboardListStateForFab

            val accountMap by remember(uiState.accounts) {
                derivedStateOf { uiState.accounts.associateBy { it.id } }
            }

            val activeOrderedCards by remember(cardOrder, activeCards) {
                derivedStateOf {
                    cardOrder.filter { it != DashboardCardType.TOTAL_BALANCE && activeCards.contains(it) }
                }
            }

            val recentTransactions by remember(uiState.recentTransactions) {
                derivedStateOf { uiState.recentTransactions }
            }

            val isRecentEmpty by remember(uiState.recentTransactions) {
                derivedStateOf { uiState.recentTransactions.isEmpty() }
            }

            LazyColumn(
                state = dashboardListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // -------------------------------------------------------------
                // PERMANENT HERO ITEM 1: Current Total Wealth Header (Pinned)
                // -------------------------------------------------------------
                item(key = "PERMANENT_HERO_TOTAL_BALANCE", contentType = "hero") {
                    TotalBalanceHeroCard(
                        totalBalance = uiState.totalBalance,
                        cashBalance = uiState.cashBalance,
                        liquidBalance = uiState.liquidBalance,
                        savingsBalance = uiState.savingsBalance,
                        assetsBalance = uiState.assetsBalance,
                        includeAssets = uiState.includeAssetsInHero,
                        baseCurrency = uiState.baseCurrency,
                        accountCount = uiState.accounts.size,
                        onAddAccountClick = { showAddAccountSheet = true },
                        onNavigateToAssets = onNavigateToAssets,
                        onToggleIncludeAssets = { viewModel.toggleIncludeAssetsInHero(!uiState.includeAssetsInHero) },
                        backgroundStyle = heroBackgroundStyle,
                        hideMoney = hideMoney,
                        onClick = { selectedCardDetail = DashboardCardType.TOTAL_BALANCE },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // -------------------------------------------------------------
                // PERMANENT HERO ITEM 2: Accounts Breakdown Row (Accounts & Sub-Accounts)
                // -------------------------------------------------------------
                item(key = "PERMANENT_ACCOUNTS_BREAKDOWN", contentType = "accounts_grid") {
                    AccountsBreakdownRow(
                        accounts = uiState.accounts,
                        cardsCount = paymentCards.size,
                        hideMoney = hideMoney,
                        onAddAccountClick = {
                            targetParentAccountIdForSubAccount = null
                            showAddAccountSheet = true
                        },
                        onAddSubAccountClick = { parentId ->
                            targetParentAccountIdForSubAccount = parentId
                            showAddAccountSheet = true
                        },
                        onAccountClick = onNavigateToAccountLog,
                        onNavigateToCards = onNavigateToCards
                    )
                }

                // -------------------------------------------------------------
                // QUICK NAVIGATION GRID: Always visible at all times, no scrolling needed
                // -------------------------------------------------------------
                item(key = "QUICK_NAVIGATION_GRID", contentType = "nav_grid") {
                    DashboardNavigationGrid(
                        onNavigateToReceiptInbox = onNavigateToReceiptInbox,
                        pendingReceiptCount = pendingReceiptCount,
                        onNavigateToCategories = onNavigateToCategories,
                        onNavigateToRecords = onNavigateToRecords,
                        onNavigateToBudgets = onNavigateToBudgets,
                        onNavigateToDebts = onNavigateToDebts,
                        onNavigateToCommission = onNavigateToCommission,
                        onNavigateToSubscriptions = onNavigateToSubscriptions,
                        onNavigateToAssets = onNavigateToAssets,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToCards = onNavigateToCards,
                        cardsCount = paymentCards.size
                    )
                }

                // -------------------------------------------------------------
                // MODULAR CARDS: Dynamically rendered based on activeCards & custom cardOrder
                // -------------------------------------------------------------
                items(
                    items = activeOrderedCards,
                    key = { "MODULAR_CARD_${it.name}" },
                    contentType = { it.name }
                ) { cardType ->
                    when (cardType) {
                        DashboardCardType.TOTAL_BALANCE -> Unit // Already rendered as Hero
                        DashboardCardType.MONEY_FLOW -> {
                            MoneyFlowCard(
                                moneyFlow = moneyFlowState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.MONEY_FLOW },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.BUDGET_PROGRESS -> {
                            BudgetPercentCard(
                                budgets = budgetState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.BUDGET_PROGRESS },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.DEBT_SUMMARY -> {
                            DebtSummaryCard(
                                topReceivables = topReceivables,
                                totalLent = totalActiveLent,
                                currencyCode = uiState.baseCurrency,
                                onViewDebtsClicked = onNavigateToDebts,
                                onClick = { selectedCardDetail = DashboardCardType.DEBT_SUMMARY },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.LIQUID_VS_LOCKED -> {
                            LiquidVsLockedCard(
                                state = liquidVsLockedState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.LIQUID_VS_LOCKED },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.PROJECT_TRACKER -> {
                            ProjectTrackerCard(
                                projectState = projectTrackerState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.PROJECT_TRACKER },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.LONG_TERM_EXPECTED -> {
                            LongTermExpectedCard(
                                expectedItems = longTermExpectedState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.LONG_TERM_EXPECTED },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.WEEKLY_FORECAST -> {
                            WeeklyForecastCard(
                                weeklyBalances = weeklyForecastState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.WEEKLY_FORECAST },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.UPCOMING_BILLS -> {
                            UpcomingBillsCard(
                                recurringBills = recurringTransactions,
                                currencyCode = uiState.baseCurrency,
                                onViewBillsClicked = onNavigateToSubscriptions,
                                onClick = { selectedCardDetail = DashboardCardType.UPCOMING_BILLS },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.SPENDING_CATEGORIES -> {
                            SpendingCategoriesCard(
                                categoryExpenses = categoryExpenses,
                                currencyCode = uiState.baseCurrency,
                                onViewCategoriesClicked = onNavigateToCategories,
                                onClick = { selectedCardDetail = DashboardCardType.SPENDING_CATEGORIES },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.SAVINGS_RATE -> {
                            SavingsRateCard(
                                moneyFlow = moneyFlowState,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.SAVINGS_RATE },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.SPENDING_TRENDS -> {
                            MonthlySpendingTrendsCard(
                                trends = monthlySpendingTrends,
                                currencyCode = uiState.baseCurrency,
                                onClick = { selectedCardDetail = DashboardCardType.SPENDING_TRENDS },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        DashboardCardType.PLANNED_PAYOUTS -> {
                            PlannedPayoutsCard(
                                plannedPayouts = plannedPayouts,
                                currencyCode = uiState.baseCurrency,
                                onViewAllClicked = onNavigateToSubscriptions,
                                onPayNowClicked = { payout: PlannedPayoutEntity ->
                                    val targetAcc = payout.targetAccountId ?: uiState.accounts.firstOrNull()?.id
                                    if (targetAcc != null) {
                                        viewModel.markPlannedPayoutAsPaid(payout, targetAcc)
                                    }
                                },
                                onClick = { selectedCardDetail = DashboardCardType.PLANNED_PAYOUTS },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // 30-Day Comprehensive Trajectory Chart
                item(key = "CASH_FLOW_FORECAST_CHART", contentType = "chart") {
                    CashFlowForecastChart(
                        dailyBalances = forecastState,
                        currencyCode = uiState.baseCurrency,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Recent Transactions Section Header
                item(key = "RECENT_TRANSACTIONS_HEADER", contentType = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent transactions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToRecords) {
                            Text("See All Records", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 12. Recent Transactions List (Starts zero-mock empty)
                if (isRecentEmpty) {
                    item(key = "EMPTY_RECENT_TRANSACTIONS", contentType = "placeholder") {
                        EmptyListPlaceholder(
                            icon = Icons.Default.ReceiptLong,
                            title = "No Transactions Logged Yet",
                            message = "Tap '+ Log Entry' to record your first income, expense, or transfer. All data starts 100% clean and offline.",
                            actionLabel = "Log Entry",
                            onAction = { showAddTransactionSheet = true },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = recentTransactions,
                        key = { "RECENT_TX_${it.id}" },
                        contentType = { "transaction_item" }
                    ) { tx ->
                        val account = accountMap[tx.accountId]
                        val accountName = account?.name ?: "Account"
                        val currencyCode = account?.currencyCode ?: uiState.baseCurrency
                        TransactionListItem(
                            transaction = tx,
                            accountName = accountName,
                            currencyCode = currencyCode,
                            hideMoney = hideMoney,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // -------------------------------------------------------------
                // 13. BOTTOM PANEL: Support Development / Buy Me a Coffee
                // -------------------------------------------------------------
                item(key = "BUY_ME_A_COFFEE_BOTTOM_PANEL", contentType = "support_panel") {
                    BuyMeACoffeeCard(
                        onOpenLink = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rohan_reyes"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showAddTransactionSheet) {
        AddTransactionSheet(
            accounts = uiState.accounts,
            categories = categories,
            baseCurrency = uiState.baseCurrency,
            onDismissRequest = { showAddTransactionSheet = false },
            onSaveTransaction = { amount, type, accountId, toAccountId, categoryId, labels, note, proofUri ->
                viewModel.addQuickTransaction(
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    labels = labels,
                    note = note,
                    proofImageUri = proofUri
                )
                showAddTransactionSheet = false
            }
        )
    }

    if (showAddAccountSheet) {
        AddAccountSheet(
            defaultCurrency = uiState.baseCurrency,
            allAccounts = uiState.accounts,
            preselectedParentAccountId = targetParentAccountIdForSubAccount,
            onDismissRequest = {
                showAddAccountSheet = false
                targetParentAccountIdForSubAccount = null
            },
            onSaveAccount = { name, type, initialBalance, color, interestRate, compFreq, currencyCode, isLocked, lockedUntil, linkedAppPackage, includeInTally, parentAccountId, subAccountType ->
                viewModel.addAccount(
                    name = name,
                    type = type,
                    initialBalance = initialBalance,
                    color = color,
                    interestRatePa = interestRate,
                    compoundingFrequency = compFreq,
                    currencyCode = currencyCode,
                    isLocked = isLocked,
                    lockedUntil = lockedUntil,
                    linkedAppPackage = linkedAppPackage,
                    includeInTally = includeInTally,
                    parentAccountId = parentAccountId,
                    subAccountType = subAccountType
                )
                showAddAccountSheet = false
                targetParentAccountIdForSubAccount = null
            }
        )
    }

    selectedCardDetail?.let { card ->
        CardDetailModalSheet(
            title = card.title,
            categoryTag = card.name.replace("_", " "),
            onDismissRequest = { selectedCardDetail = null }
        ) {
            when (card) {
                DashboardCardType.TOTAL_BALANCE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Liquid ${CurrencyUtils.formatCurrency(uiState.liquidBalance, uiState.baseCurrency)} · Savings ${CurrencyUtils.formatCurrency(uiState.savingsBalance, uiState.baseCurrency)} · Assets ${CurrencyUtils.formatCurrency(uiState.assetsBalance, uiState.baseCurrency)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You currently have ${uiState.accounts.size} active Accounts and Sub-Accounts configured. All amounts are calculated dynamically from your verified cleared ledger.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.accounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = acc.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${acc.type} • ${acc.currencyCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyUtils.formatCurrency(acc.currentBalance, acc.currencyCode),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                DashboardCardType.MONEY_FLOW -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Cash Inflow & Outflow Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Income: +${CurrencyUtils.formatCurrency(moneyFlowState.totalIncome, uiState.baseCurrency)}\nTotal Expenses: -${CurrencyUtils.formatCurrency(moneyFlowState.totalExpense, uiState.baseCurrency)}\nNet Position: ${if (moneyFlowState.netCashFlow >= 0) "+" else ""}${CurrencyUtils.formatCurrency(moneyFlowState.netCashFlow, uiState.baseCurrency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                selectedCardDetail = null
                                onNavigateToRecords()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Full Transaction Ledger →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                DashboardCardType.BUDGET_PROGRESS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Active Budget Limits & Ceilings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (budgetState.isEmpty()) {
                            Text(
                                text = "No category budgets currently established. Setting spending limits helps preserve monthly savings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            budgetState.forEach { b ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = b.categoryName, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Spent ${CurrencyUtils.formatCurrency(b.currentSpent, uiState.baseCurrency)} of ${CurrencyUtils.formatCurrency(b.amountLimit, uiState.baseCurrency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                selectedCardDetail = null
                                onNavigateToBudgets()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Category Budgets →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                DashboardCardType.DEBT_SUMMARY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Receivables & Borrowed Debts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Active Lent to Others: ${CurrencyUtils.formatCurrency(totalActiveLent, uiState.baseCurrency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        topReceivables.forEach { debt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = debt.personName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = CurrencyUtils.formatCurrency(debt.remainingAmount, uiState.baseCurrency),
                                    fontWeight = FontWeight.Bold,
                                    color = FinPositive
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                selectedCardDetail = null
                                onNavigateToDebts()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Debts & Loans Manager →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                DashboardCardType.LIQUID_VS_LOCKED -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Liquidity & Vault Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Immediately Spendable: ${CurrencyUtils.formatCurrency(liquidVsLockedState.spendableLiquid, uiState.baseCurrency)}\n• Time-Deposits / Locked Vaults: ${CurrencyUtils.formatCurrency(liquidVsLockedState.lockedSavings, uiState.baseCurrency)}\n• Overall Liquidity: ${(liquidVsLockedState.liquidRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DashboardCardType.PROJECT_TRACKER -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Tagged Initiative Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Initiative Tag: #${projectTrackerState.labelName}\nNet Balance: ${CurrencyUtils.formatCurrency(projectTrackerState.netBalance, uiState.baseCurrency)}\nTotal Entries: ${projectTrackerState.transactionCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DashboardCardType.LONG_TERM_EXPECTED -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Projected Long-Term Inflows",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (longTermExpectedState.isEmpty()) {
                            Text(
                                text = "No delayed receivables or upcoming project milestones scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            longTermExpectedState.forEach { exp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = exp.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = exp.sourceOrPerson,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "+" + CurrencyUtils.formatCurrency(exp.amount, uiState.baseCurrency),
                                        fontWeight = FontWeight.Bold,
                                        color = FinPositive
                                    )
                                }
                            }
                        }
                    }
                }
                DashboardCardType.WEEKLY_FORECAST -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "7-Day Projected Trajectory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Calculated by projecting confirmed recurring bills and scheduled planned revenues against current cleared balances.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        weeklyForecastState.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = day.dayLabel, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = CurrencyUtils.formatCurrency(day.projectedBalance, uiState.baseCurrency),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                DashboardCardType.UPCOMING_BILLS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Subscriptions & Commitments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (recurringTransactions.isEmpty()) {
                            Text(
                                text = "No recurring bills or subscriptions scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            recurringTransactions.forEach { bill ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = bill.note ?: "Bill", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${bill.recurrenceRule ?: "Monthly"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "-" + CurrencyUtils.formatCurrency(bill.amount, uiState.baseCurrency),
                                        fontWeight = FontWeight.Bold,
                                        color = FinNegative
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                selectedCardDetail = null
                                onNavigateToSubscriptions()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Subscriptions & Bills →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                DashboardCardType.SPENDING_CATEGORIES -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Category Expense Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (categoryExpenses.isEmpty()) {
                            Text(
                                text = "No category expense entries logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            categoryExpenses.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = cat.categoryName, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = CurrencyUtils.formatCurrency(cat.totalAmount, uiState.baseCurrency),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                selectedCardDetail = null
                                onNavigateToCategories()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View All Categories →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                DashboardCardType.SAVINGS_RATE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val rate = if (moneyFlowState.totalIncome > 0) {
                            ((moneyFlowState.totalIncome - moneyFlowState.totalExpense) / moneyFlowState.totalIncome * 100)
                        } else 0.0
                        Text(
                            text = "Personal Savings Efficiency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Savings Rate: ${String.format(Locale.getDefault(), "%.1f%%", rate)}\n• Gross Earned: ${CurrencyUtils.formatCurrency(moneyFlowState.totalIncome, uiState.baseCurrency)}\n• Gross Spent: ${CurrencyUtils.formatCurrency(moneyFlowState.totalExpense, uiState.baseCurrency)}\n• Retained Capital: ${CurrencyUtils.formatCurrency(moneyFlowState.netCashFlow, uiState.baseCurrency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DashboardCardType.SPENDING_TRENDS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "6-Month Historical Spending Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        monthlySpendingTrends.reversed().forEach { trend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${trend.monthLabel} ${trend.year}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${trend.transactionCount} transactions · ~${CurrencyUtils.formatCurrency(trend.averageDailySpending, uiState.baseCurrency)}/day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Spent: " + CurrencyUtils.formatCurrency(trend.totalExpense, uiState.baseCurrency),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    if (trend.totalIncome > 0) {
                                        Text(
                                            text = "Earned: " + CurrencyUtils.formatCurrency(trend.totalIncome, uiState.baseCurrency),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FinPositive
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                DashboardCardType.PLANNED_PAYOUTS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Scheduled One-Off Commitments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val pending = plannedPayouts.filter { it.status.equals("PENDING", ignoreCase = true) }
                        Text(
                            text = "Total scheduled: ${CurrencyUtils.formatCurrency(pending.sumOf { it.amount }, uiState.baseCurrency)} across ${pending.size} upcoming commitment(s). One-off payouts do not recur and are distinct from debts and freelance milestones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                selectedCardDetail = null
                                onNavigateToSubscriptions()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Commitments & Payouts Hub →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Navigation Grid located directly beneath the Accounts Breakdown.
 * All 8 modules (Receipts, Ledger, Budgets, Assets, Categories, Debts, Freelance, Bills) are visible at all times without scrolling.
 */
/**
 * Permanent Hero Item 1: Total Balance / Net Worth Card with interactive Asset inclusion toggle.
 */
@Composable
fun TotalBalanceHeroCard(
    totalBalance: Double,
    baseCurrency: String,
    accountCount: Int,
    onAddAccountClick: () -> Unit,
    cashBalance: Double = totalBalance,
    liquidBalance: Double = cashBalance,
    savingsBalance: Double = 0.0,
    assetsBalance: Double = 0.0,
    includeAssets: Boolean = true,
    onToggleIncludeAssets: (() -> Unit)? = null,
    onNavigateToAssets: (() -> Unit)? = null,
    backgroundStyle: HeroBackgroundStyle = HeroBackgroundStyle.SOLID,
    hideMoney: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isGradient = backgroundStyle != HeroBackgroundStyle.SOLID

    val gradientBrush = remember(backgroundStyle) {
        when (backgroundStyle) {
            HeroBackgroundStyle.GRADIENT -> Brush.linearGradient(
                colors = listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF0072FF))
            )
            HeroBackgroundStyle.SUNSET_GLOW -> Brush.linearGradient(
                colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
            )
            HeroBackgroundStyle.EMERALD_AURORA -> Brush.linearGradient(
                colors = listOf(Color(0xFF0575E6), Color(0xFF00B4DB), Color(0xFF00F260))
            )
            HeroBackgroundStyle.MIDNIGHT_NEBULA -> Brush.linearGradient(
                colors = listOf(Color(0xFF200122), Color(0xFF6f0000), Color(0xFF9013FE))
            )
            HeroBackgroundStyle.GOLDEN_LUXE -> Brush.linearGradient(
                colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
            )
            HeroBackgroundStyle.SOLID -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        }
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isGradient) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isGradient) {
                    Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(gradientBrush)
                } else Modifier
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.6f,
                    center = Offset(size.width * 0.92f, size.height * 0.12f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width * 0.12f, size.height * 0.9f)
                )
            }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        val contentColor = if (isGradient) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isGradient)
                                    Color.White.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            tint = if (isGradient) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (includeAssets) "NET WORTH" else "LIQUID FUNDS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor.copy(alpha = 0.78f),
                            letterSpacing = 0.9.sp
                        )
                        Text(
                            text = if (includeAssets) "Liquid + savings + assets" else "Spendable now",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGradient)
                        Color.White.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = baseCurrency,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isGradient) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = CurrencyUtils.formatCurrency(totalBalance, baseCurrency, hideMoney),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )

            // Liquid / savings / assets breakdown. Tapping switches between liquid funds and net worth.
            val chipColor = if (isGradient) Color.Black.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            val chips = buildList {
                if (includeAssets) add("Liquid ${CurrencyUtils.formatCurrency(liquidBalance, baseCurrency, hideMoney)}")
                if (savingsBalance != 0.0) add("Savings ${CurrencyUtils.formatCurrency(savingsBalance, baseCurrency, hideMoney)}")
                if (includeAssets && assetsBalance != 0.0) add("Assets ${CurrencyUtils.formatCurrency(assetsBalance, baseCurrency, hideMoney)}")
            }
            val canToggle = onToggleIncludeAssets != null && (assetsBalance != 0.0 || savingsBalance != 0.0)
            if (chips.isNotEmpty() || canToggle) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    chips.forEach { label ->
                        Surface(shape = RoundedCornerShape(8.dp), color = chipColor) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (canToggle) {
                        Surface(
                            onClick = onToggleIncludeAssets!!,
                            shape = RoundedCornerShape(8.dp),
                            color = if (isGradient) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (includeAssets) "Show liquid only" else "Show net worth",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (accountCount == 0) "No active accounts" else "$accountCount accounts connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.82f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onNavigateToAssets != null) {
                        Surface(
                            onClick = onNavigateToAssets,
                            shape = RoundedCornerShape(10.dp),
                            color = if (isGradient) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.testTag("hero_assets_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isGradient) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Assets",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGradient) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = onAddAccountClick,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGradient) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("hero_add_account_button")
                    ) {
                        Text(
                            text = "+ Add Account",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGradient) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Permanent Hero Item 2: Compact Accounts & Sub-Accounts Quick Cards (Visible at a glance).
 */
/**
 * Legacy alias for backwards compatibility
 */
@Composable
fun AccountMiniCard(
    account: AccountEntity,
    hideMoney: Boolean = false,
    subAccountCount: Int = 0,
    onAddSubAccount: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactAccountCard(
        account = account,
        hideMoney = hideMoney,
        onAddSubAccount = onAddSubAccount,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TransactionListItem(
    transaction: TransactionEntity,
    accountName: String,
    currencyCode: String,
    hideMoney: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isIncome = remember(transaction.type) { transaction.type.equals(TransactionType.INCOME.displayName, ignoreCase = true) }
    val isTransfer = remember(transaction.type) { transaction.type.equals(TransactionType.TRANSFER.displayName, ignoreCase = true) }

    val typeColor = remember(isIncome, isTransfer) {
        when {
            isIncome -> FinPositive
            isTransfer -> Color(0xFF0091EA)
            else -> FinNegative
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
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(transaction.date)
    }

    val formattedAmount = remember(transaction.amount, currencyCode, hideMoney, isIncome, isTransfer) {
        val amt = CurrencyUtils.formatCurrency(transaction.amount, currencyCode, hideMoney)
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                Text(
                    text = transaction.note?.takeIf { it.isNotBlank() } ?: transaction.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (transaction.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        transaction.labels.forEach { label ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "#$label",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor
                )

                if (transaction.status.equals("Planned", ignoreCase = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Planned",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Panel: Buy Me a Coffee Support Card
 */
@Composable
fun BuyMeACoffeeCard(
    onOpenLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFDD00),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.LocalCafe, contentDescription = null, tint = Color(0xFF1E1E1E), modifier = Modifier.size(24.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Buy Me a Coffee",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Support ongoing offline-first development & updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "buymeacoffee.com/rohan_reyes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Creator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onOpenLink,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFDD00),
                    contentColor = Color(0xFF1E1E1E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_buy_me_a_coffee_button")
            ) {
                Text(
                    text = "Buy Me a Coffee",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
