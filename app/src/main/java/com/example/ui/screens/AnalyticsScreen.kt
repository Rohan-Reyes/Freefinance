package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.MilestoneEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.AccountType
import com.example.data.local.model.DebtStatus
import com.example.data.local.model.DebtType
import com.example.data.local.model.ProjectWithMilestones
import com.example.data.local.model.TransactionStatus
import com.example.data.local.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class AnalyticsTab(val title: String, val icon: ImageVector) {
    CALENDAR("Calendar & Flow", Icons.Default.CalendarMonth),
    FORECAST("Cashflow Forecast", Icons.Default.Timeline),
    VARIANCE("Category Shifts", Icons.AutoMirrored.Filled.TrendingUp),
    HEALTH("Health Score", Icons.Default.HealthAndSafety)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allBudgets by viewModel.allBudgets.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val allDebts by viewModel.allDebts.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val freelanceProjects by viewModel.freelanceProjects.collectAsState()

    var selectedTab by remember { mutableStateOf(AnalyticsTab.CALENDAR) }

    // Selected Month & Year (defaults to current month)
    var selectedCalendarMonth by remember {
        val cal = Calendar.getInstance()
        mutableStateOf(cal.get(Calendar.MONTH))
    }
    var selectedCalendarYear by remember {
        val cal = Calendar.getInstance()
        mutableStateOf(cal.get(Calendar.YEAR))
    }

    var selectedDayInMonth by remember {
        val cal = Calendar.getInstance()
        mutableStateOf<Int?>(cal.get(Calendar.DAY_OF_MONTH))
    }

    val monthName = remember(selectedCalendarMonth, selectedCalendarYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedCalendarMonth)
            set(Calendar.YEAR, selectedCalendarYear)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Filter transactions for the selected month (Cleared actual transactions only)
    val monthTransactions by remember(allTransactions, selectedCalendarMonth, selectedCalendarYear) {
        derivedStateOf {
            allTransactions.filter { tx ->
                val isCleared = tx.status.equals("Cleared", ignoreCase = true) || tx.status.equals(TransactionStatus.CLEARED.displayName, ignoreCase = true)
                val cal = Calendar.getInstance().apply { time = tx.date }
                isCleared && cal.get(Calendar.MONTH) == selectedCalendarMonth && cal.get(Calendar.YEAR) == selectedCalendarYear
            }
        }
    }

    // Previous month transactions for MoM comparison (Cleared actual transactions only)
    val prevMonthTransactions by remember(allTransactions, selectedCalendarMonth, selectedCalendarYear) {
        derivedStateOf {
            val prevCal = Calendar.getInstance().apply {
                set(Calendar.MONTH, selectedCalendarMonth)
                set(Calendar.YEAR, selectedCalendarYear)
                add(Calendar.MONTH, -1)
            }
            val pMonth = prevCal.get(Calendar.MONTH)
            val pYear = prevCal.get(Calendar.YEAR)
            allTransactions.filter { tx ->
                val isCleared = tx.status.equals("Cleared", ignoreCase = true) || tx.status.equals(TransactionStatus.CLEARED.displayName, ignoreCase = true)
                val cal = Calendar.getInstance().apply { time = tx.date }
                isCleared && cal.get(Calendar.MONTH) == pMonth && cal.get(Calendar.YEAR) == pYear
            }
        }
    }

    val monthIncome by remember(monthTransactions) {
        derivedStateOf {
            monthTransactions.filter { it.type == TransactionType.INCOME.displayName }.sumOf { it.amount }
        }
    }

    val monthExpense by remember(monthTransactions) {
        derivedStateOf {
            monthTransactions.filter { it.type == TransactionType.EXPENSE.displayName }.sumOf { it.amount }
        }
    }

    val netMonthSavings by remember(monthIncome, monthExpense) {
        derivedStateOf { monthIncome - monthExpense }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics & Forecast",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("analytics_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Month Picker Bar
            MonthSelectorHeader(
                monthLabel = monthName,
                onPreviousMonth = {
                    if (selectedCalendarMonth == 0) {
                        selectedCalendarMonth = 11
                        selectedCalendarYear -= 1
                    } else {
                        selectedCalendarMonth -= 1
                    }
                    selectedDayInMonth = null
                },
                onNextMonth = {
                    if (selectedCalendarMonth == 11) {
                        selectedCalendarMonth = 0
                        selectedCalendarYear += 1
                    } else {
                        selectedCalendarMonth += 1
                    }
                    selectedDayInMonth = null
                },
                onResetCurrentMonth = {
                    val now = Calendar.getInstance()
                    selectedCalendarMonth = now.get(Calendar.MONTH)
                    selectedCalendarYear = now.get(Calendar.YEAR)
                    selectedDayInMonth = now.get(Calendar.DAY_OF_MONTH)
                }
            )

            // Top Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                AnalyticsTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                AnalyticsTab.CALENDAR -> {
                    CalendarHeatmapTab(
                        year = selectedCalendarYear,
                        month = selectedCalendarMonth,
                        monthIncome = monthIncome,
                        monthExpense = monthExpense,
                        netSavings = netMonthSavings,
                        monthTransactions = monthTransactions,
                        recurringBills = recurringTransactions,
                        allDebts = allDebts,
                        freelanceProjects = freelanceProjects,
                        allCategories = allCategories,
                        allAccounts = allAccounts,
                        baseCurrency = baseCurrency,
                        selectedDay = selectedDayInMonth,
                        onDaySelected = { day -> selectedDayInMonth = day }
                    )
                }
                AnalyticsTab.FORECAST -> {
                    CashflowForecastTab(
                        allAccounts = allAccounts,
                        monthTransactions = monthTransactions,
                        recurringTransactions = recurringTransactions,
                        allDebts = allDebts,
                        freelanceProjects = freelanceProjects,
                        baseCurrency = baseCurrency,
                        selectedMonth = selectedCalendarMonth,
                        selectedYear = selectedCalendarYear
                    )
                }
                AnalyticsTab.VARIANCE -> {
                    CategoryVarianceTab(
                        currentMonthTxs = monthTransactions,
                        prevMonthTxs = prevMonthTransactions,
                        allCategories = allCategories,
                        baseCurrency = baseCurrency,
                        currentMonthLabel = monthName
                    )
                }
                AnalyticsTab.HEALTH -> {
                    FinancialHealthScoreTab(
                        allAccounts = allAccounts,
                        allTransactions = allTransactions,
                        monthIncome = monthIncome,
                        monthExpense = monthExpense,
                        allBudgets = allBudgets,
                        allDebts = allDebts,
                        baseCurrency = baseCurrency
                    )
                }
            }
        }
    }
}

/**
 * Header bar for moving between months
 */
@Composable
private fun MonthSelectorHeader(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable { onResetCurrentMonth() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }
    }
}

/**
 * Tab 1: Calendar Heatmap & Day Breakdown
 */
data class MilestoneWithProject(
    val milestone: MilestoneEntity,
    val projectName: String,
    val client: String
)

@Composable
private fun CalendarHeatmapTab(
    year: Int,
    month: Int,
    monthIncome: Double,
    monthExpense: Double,
    netSavings: Double,
    monthTransactions: List<TransactionEntity>,
    recurringBills: List<TransactionEntity>,
    allDebts: List<DebtEntity> = emptyList(),
    freelanceProjects: List<ProjectWithMilestones> = emptyList(),
    allCategories: List<CategoryEntity>,
    allAccounts: List<AccountEntity>,
    baseCurrency: String,
    selectedDay: Int?,
    onDaySelected: (Int?) -> Unit
) {
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

    // Aggregate spending & income per day of the month
    val dailyData = remember(monthTransactions, month, year) {
        val map = mutableMapOf<Int, Pair<Double, Double>>() // Day -> (Income, Expense)
        monthTransactions.forEach { tx ->
            val cal = Calendar.getInstance().apply { time = tx.date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val current = map[day] ?: (0.0 to 0.0)
            when (tx.type) {
                TransactionType.INCOME.displayName -> map[day] = (current.first + tx.amount) to current.second
                TransactionType.EXPENSE.displayName -> map[day] = current.first to (current.second + tx.amount)
            }
        }
        map
    }

    val maxDailyExpense = remember(dailyData) {
        dailyData.values.maxOfOrNull { it.second } ?: 1.0
    }

    // Days count and starting day of week
    val (daysInMonth, firstDayOfWeek) = remember(month, year) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDay = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
        maxDays to startDay
    }

    // Map active recurring bills and inflows to their due days in the month
    val recurringDaysMap = remember(recurringBills, daysInMonth, month, year) {
        val map = mutableMapOf<Int, MutableList<TransactionEntity>>()
        recurringBills.filter { bill ->
            val isPlanned = bill.status.equals("Planned", ignoreCase = true)
            val isRecurring = !bill.recurrenceRule.isNullOrBlank() && !bill.recurrenceRule.equals("None", ignoreCase = true)
            val cal = Calendar.getInstance().apply { time = bill.date }
            val billYear = cal.get(Calendar.YEAR)
            val billMonth = cal.get(Calendar.MONTH)
            isPlanned && isRecurring && (billYear < year || (billYear == year && billMonth <= month))
        }.forEach { bill ->
            val occurrences = com.example.util.RecurrenceHelper.getOccurrencesInMonth(
                rule = bill.recurrenceRule,
                startDate = bill.date,
                month = month,
                year = year
            )
            occurrences.forEach { day ->
                if (day in 1..daysInMonth) {
                    map.getOrPut(day) { mutableListOf() }.add(bill)
                }
            }
        }
        map
    }

    // Map expected debt payout dates for active debts
    val debtDaysMap = remember(allDebts, month, year) {
        val map = mutableMapOf<Int, MutableList<DebtEntity>>()
        allDebts.filter { it.status.equals("Active", ignoreCase = true) && it.dueDate != null }.forEach { debt ->
            val cal = Calendar.getInstance().apply { time = debt.dueDate!! }
            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(debt)
            }
        }
        map
    }

    // Map expected freelance milestone payouts for pending milestones
    val freelanceDaysMap = remember(freelanceProjects, month, year) {
        val map = mutableMapOf<Int, MutableList<MilestoneWithProject>>()
        freelanceProjects.forEach { proj ->
            proj.milestones.filter {
                !it.status.equals("Paid", ignoreCase = true)
            }.forEach { milestone ->
                val cal = Calendar.getInstance().apply { time = milestone.expectedDate }
                if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    map.getOrPut(day) { mutableListOf() }.add(
                        MilestoneWithProject(
                            milestone = milestone,
                            projectName = proj.project.projectName,
                            client = proj.project.client
                        )
                    )
                }
            }
        }
        map
    }

    val selectedDayTransactions = remember(monthTransactions, selectedDay) {
        if (selectedDay == null) emptyList()
        else {
            monthTransactions.filter { tx ->
                val cal = Calendar.getInstance().apply { time = tx.date }
                cal.get(Calendar.DAY_OF_MONTH) == selectedDay
            }
        }
    }

    val selectedDayRecurring = remember(recurringDaysMap, selectedDay) {
        if (selectedDay == null) emptyList()
        else recurringDaysMap[selectedDay] ?: emptyList()
    }

    val selectedDayDebts = remember(debtDaysMap, selectedDay) {
        if (selectedDay == null) emptyList()
        else debtDaysMap[selectedDay] ?: emptyList()
    }

    val selectedDayFreelance = remember(freelanceDaysMap, selectedDay) {
        if (selectedDay == null) emptyList()
        else freelanceDaysMap[selectedDay] ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Financial Overview Summary
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Inflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "+${CurrencyUtils.formatCurrency(monthIncome, baseCurrency)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Outflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "-${CurrencyUtils.formatCurrency(monthExpense, baseCurrency)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = (if (netSavings >= 0) "+" else "") + CurrencyUtils.formatCurrency(netSavings, baseCurrency),
                            fontWeight = FontWeight.Bold,
                            color = if (netSavings >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Calendar Heatmap Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Cash Activity Heatmap",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00C853)))
                            Text("In", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                            Text("Out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF7C4DFF)))
                            Text("Milestone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                            Text("Debt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        dayNames.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Calendar Days Grid (Calculated in rows)
                    val totalCells = firstDayOfWeek + daysInMonth
                    val totalRows = (totalCells + 6) / 7

                    for (rowIndex in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (colIndex in 0..6) {
                                val cellIndex = rowIndex * 7 + colIndex
                                val dayNumber = cellIndex - firstDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val dayRecord = dailyData[dayNumber]
                                    val hasIncome = (dayRecord?.first ?: 0.0) > 0
                                    val expenseAmount = dayRecord?.second ?: 0.0
                                    val hasExpense = expenseAmount > 0
                                    val hasRecurring = recurringDaysMap.containsKey(dayNumber)
                                    val dayDebts = debtDaysMap[dayNumber] ?: emptyList()
                                    val hasDebts = dayDebts.isNotEmpty()
                                    val dayFreelance = freelanceDaysMap[dayNumber] ?: emptyList()
                                    val hasFreelance = dayFreelance.isNotEmpty()
                                    val isSelected = selectedDay == dayNumber

                                    // Compute heat tint based on expense intensity
                                    val heatAlpha = if (hasExpense && maxDailyExpense > 0) {
                                        min(0.8f, max(0.15f, (expenseAmount / maxDailyExpense).toFloat()))
                                    } else 0f

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(2.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                    hasExpense -> MaterialTheme.colorScheme.error.copy(alpha = heatAlpha)
                                                    hasFreelance -> Color(0xFF7C4DFF).copy(alpha = 0.2f)
                                                    hasIncome -> Color(0xFF00C853).copy(alpha = 0.2f)
                                                    hasRecurring -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    hasDebts -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                onDaySelected(if (isSelected) null else dayNumber)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                fontWeight = if (isSelected || hasExpense || hasIncome || hasRecurring || hasDebts || hasFreelance) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    hasExpense && heatAlpha > 0.5f -> Color.White
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasIncome) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF00C853))
                                                    )
                                                }
                                                if (hasFreelance) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF7C4DFF))
                                                    )
                                                }
                                                if (hasRecurring) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                                if (hasDebts) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFFF9800))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).padding(2.dp).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Activity Details
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDay != null) "Activity for Day $selectedDay" else "Tap a day on the calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedDay != null) {
                            val dayExp = dailyData[selectedDay]?.second ?: 0.0
                            Text(
                                text = "Spent: ${CurrencyUtils.formatCurrency(dayExp, baseCurrency)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (selectedDay == null) {
                        Text(
                            text = "Select any date in the heatmap above to view logged receipts, transfers, scheduled recurring bills, expected freelance payouts, and debt due dates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (selectedDayTransactions.isEmpty() && selectedDayRecurring.isEmpty() && selectedDayDebts.isEmpty() && selectedDayFreelance.isEmpty()) {
                        Text(
                            text = "No transactions, scheduled payments, freelance milestones, or debt payouts on this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Expected Freelance Milestone Payouts for this day
                        if (selectedDayFreelance.isNotEmpty()) {
                            Text(
                                text = "Expected Freelance Payouts (Milestones)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7C4DFF)
                            )
                            selectedDayFreelance.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF7C4DFF).copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${item.projectName} · ${item.milestone.title}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Client: ${item.client} · Status: ${item.milestone.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "+${CurrencyUtils.formatCurrency(item.milestone.amount, baseCurrency)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF7C4DFF)
                                        )
                                    }
                                }
                            }
                        }

                        // Expected Debt Payouts & Collections for this day
                        if (selectedDayDebts.isNotEmpty()) {
                            Text(
                                text = "Expected Debt Payouts / Due Dates",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9800),
                                modifier = if (selectedDayFreelance.isNotEmpty()) Modifier.padding(top = 4.dp) else Modifier
                            )
                            selectedDayDebts.forEach { debt ->
                                val isLent = debt.type.equals("Lent", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isLent) Color(0xFF00C853).copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${debt.personName} (${if (isLent) "Lent / Expected Return" else "Borrowed / Due Repayment"})",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = if (debt.notes.isNullOrBlank()) "Expected payout day" else debt.notes,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = (if (isLent) "+" else "-") + CurrencyUtils.formatCurrency(debt.remainingAmount, baseCurrency),
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isLent) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        // Active Scheduled Inflows & Bills for this day
                        if (selectedDayRecurring.isNotEmpty()) {
                            Text(
                                text = "Scheduled Recurring Items",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = if (selectedDayDebts.isNotEmpty() || selectedDayFreelance.isNotEmpty()) Modifier.padding(top = 4.dp) else Modifier
                            )
                            selectedDayRecurring.forEach { bill ->
                                val isIncome = bill.type.equals(TransactionType.INCOME.displayName, ignoreCase = true)
                                val category = bill.categoryId?.let { categoryMap[it] }
                                val account = accountMap[bill.accountId]
                                val scheduleDesc = com.example.util.RecurrenceHelper.formatSchedule(bill.recurrenceRule, bill.date)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isIncome) Color(0xFF00C853).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (bill.note.isNullOrBlank()) (category?.name ?: if (isIncome) "Scheduled Inflow" else "Recurring Bill") else bill.note,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "$scheduleDesc${if (account != null) " · ${account.name}" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = (if (isIncome) "+" else "-") + CurrencyUtils.formatCurrency(bill.amount, baseCurrency),
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isIncome) Color(0xFF00C853) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Cleared logged transactions for this day
                        if (selectedDayTransactions.isNotEmpty()) {
                            if (selectedDayRecurring.isNotEmpty() || selectedDayDebts.isNotEmpty() || selectedDayFreelance.isNotEmpty()) {
                                Text(
                                    text = "Logged Transactions",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            selectedDayTransactions.forEach { tx ->
                                val category = tx.categoryId?.let { categoryMap[it] }
                                val account = accountMap[tx.accountId]

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (tx.note.isNullOrBlank()) (category?.name ?: "Transaction") else tx.note,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${account?.name ?: "Account"} · ${category?.name ?: tx.type}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = (if (tx.type == TransactionType.INCOME.displayName) "+" else "-") +
                                                    CurrencyUtils.formatCurrency(tx.amount, baseCurrency),
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (tx.type == TransactionType.INCOME.displayName) Color(0xFF00C853) else MaterialTheme.colorScheme.error
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
}

/**
 * Tab 2: Cashflow Forecasting Engine
 */
@Composable
private fun CashflowForecastTab(
    allAccounts: List<AccountEntity>,
    monthTransactions: List<TransactionEntity>,
    recurringTransactions: List<TransactionEntity>,
    allDebts: List<DebtEntity> = emptyList(),
    freelanceProjects: List<ProjectWithMilestones> = emptyList(),
    baseCurrency: String,
    selectedMonth: Int,
    selectedYear: Int
) {
    val totalLiquidBalance = remember(allAccounts) {
        allAccounts.filter { it.includeInTally && !it.isLocked }.sumOf { it.currentBalance }
    }

    // Days calculation for projection
    val cal = Calendar.getInstance()
    val isCurrentMonth = cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
    val currentDay = if (isCurrentMonth) cal.get(Calendar.DAY_OF_MONTH) else 1
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysRemaining = max(1, totalDaysInMonth - currentDay + 1)

    // Current month expenses so far
    val totalExpensesSoFar = remember(monthTransactions) {
        monthTransactions.filter { it.type == TransactionType.EXPENSE.displayName }.sumOf { it.amount }
    }

    val dailyBurnRate = remember(totalExpensesSoFar, currentDay) {
        if (currentDay > 0) totalExpensesSoFar / currentDay else 0.0
    }

    // Recurring expenses & recurring inflows remaining in the month (using RecurrenceHelper)
    val remainingMonthRecurringExpense = remember(recurringTransactions, currentDay, selectedMonth, selectedYear) {
        recurringTransactions.filter { tx ->
            val isPlanned = tx.status.equals("Planned", ignoreCase = true)
            val isRecurring = !tx.recurrenceRule.isNullOrBlank() && !tx.recurrenceRule.equals("None", ignoreCase = true)
            val isExpense = tx.type.equals(TransactionType.EXPENSE.displayName, ignoreCase = true)
            isPlanned && isRecurring && isExpense
        }.sumOf { tx ->
            com.example.util.RecurrenceHelper.calculateRemainingMonthlyAmount(
                amount = tx.amount,
                recurrenceRule = tx.recurrenceRule,
                startDate = tx.date,
                currentDay = currentDay,
                month = selectedMonth,
                year = selectedYear
            )
        }
    }

    val remainingMonthRecurringIncome = remember(recurringTransactions, currentDay, selectedMonth, selectedYear) {
        recurringTransactions.filter { tx ->
            val isPlanned = tx.status.equals("Planned", ignoreCase = true)
            val isRecurring = !tx.recurrenceRule.isNullOrBlank() && !tx.recurrenceRule.equals("None", ignoreCase = true)
            val isIncome = tx.type.equals(TransactionType.INCOME.displayName, ignoreCase = true)
            isPlanned && isRecurring && isIncome
        }.sumOf { tx ->
            com.example.util.RecurrenceHelper.calculateRemainingMonthlyAmount(
                amount = tx.amount,
                recurrenceRule = tx.recurrenceRule,
                startDate = tx.date,
                currentDay = currentDay,
                month = selectedMonth,
                year = selectedYear
            )
        }
    }

    // Active debts due in the remaining days of this month
    val remainingMonthLentDebts = remember(allDebts, currentDay, selectedMonth, selectedYear) {
        allDebts.filter {
            it.status.equals("Active", ignoreCase = true) &&
                    it.type.equals("Lent", ignoreCase = true) &&
                    it.dueDate != null &&
                    Calendar.getInstance().apply { time = it.dueDate!! }.let { c ->
                        c.get(Calendar.MONTH) == selectedMonth &&
                                c.get(Calendar.YEAR) == selectedYear &&
                                c.get(Calendar.DAY_OF_MONTH) >= currentDay
                    }
        }
    }

    val remainingMonthBorrowedDebts = remember(allDebts, currentDay, selectedMonth, selectedYear) {
        allDebts.filter {
            it.status.equals("Active", ignoreCase = true) &&
                    it.type.equals("Borrowed", ignoreCase = true) &&
                    it.dueDate != null &&
                    Calendar.getInstance().apply { time = it.dueDate!! }.let { c ->
                        c.get(Calendar.MONTH) == selectedMonth &&
                                c.get(Calendar.YEAR) == selectedYear &&
                                c.get(Calendar.DAY_OF_MONTH) >= currentDay
                    }
        }
    }

    // Pending freelance milestone receivables in the remaining days of this month
    val remainingMonthFreelanceMilestones = remember(freelanceProjects, currentDay, selectedMonth, selectedYear) {
        freelanceProjects.flatMap { it.milestones }.filter {
            !it.status.equals("Paid", ignoreCase = true) &&
                    Calendar.getInstance().apply { time = it.expectedDate }.let { c ->
                        c.get(Calendar.MONTH) == selectedMonth &&
                                c.get(Calendar.YEAR) == selectedYear &&
                                c.get(Calendar.DAY_OF_MONTH) >= currentDay
                    }
        }
    }

    val totalExpectedDebtReceivable = remember(remainingMonthLentDebts) {
        remainingMonthLentDebts.sumOf { it.remainingAmount }
    }

    val totalExpectedDebtPayable = remember(remainingMonthBorrowedDebts) {
        remainingMonthBorrowedDebts.sumOf { it.remainingAmount }
    }

    val totalExpectedFreelanceReceivable = remember(remainingMonthFreelanceMilestones) {
        remainingMonthFreelanceMilestones.sumOf { it.amount }
    }

    // Daily safe-to-spend allowance
    val safeDailyAllowance = remember(totalLiquidBalance, remainingMonthRecurringIncome, remainingMonthRecurringExpense, totalExpectedDebtPayable, daysRemaining) {
        val spendableBudget = max(0.0, totalLiquidBalance + remainingMonthRecurringIncome - remainingMonthRecurringExpense - totalExpectedDebtPayable)
        spendableBudget / daysRemaining
    }

    // Projected Month-End balance (including expected freelance milestone receivables, recurring inflows, recurring bills, and debt payouts)
    val projectedMonthEndBalance = remember(
        totalLiquidBalance,
        dailyBurnRate,
        daysRemaining,
        remainingMonthRecurringIncome,
        remainingMonthRecurringExpense,
        totalExpectedDebtReceivable,
        totalExpectedFreelanceReceivable,
        totalExpectedDebtPayable
    ) {
        totalLiquidBalance - (dailyBurnRate * daysRemaining) + remainingMonthRecurringIncome + totalExpectedDebtReceivable + totalExpectedFreelanceReceivable - remainingMonthRecurringExpense - totalExpectedDebtPayable
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Forecast Metric
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Month-End Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "$daysRemaining days left",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = CurrencyUtils.formatCurrency(projectedMonthEndBalance, baseCurrency),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Estimated cash balance on Day $totalDaysInMonth accounting for velocity, recurring bills, freelance milestone payouts & debt payouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Safe Daily Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatCurrency(safeDailyAllowance, baseCurrency),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00C853),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Current Daily Burn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatCurrency(dailyBurnRate, baseCurrency),
                                    fontWeight = FontWeight.Bold,
                                    color = if (dailyBurnRate > safeDailyAllowance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cashflow Runway & Commitment Breakdown
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Cashflow Velocity & Commitments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Velocity Gauge Comparison
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Burn vs Safe Allowance Ratio", style = MaterialTheme.typography.bodySmall)
                            val ratio = if (safeDailyAllowance > 0) (dailyBurnRate / safeDailyAllowance * 100).roundToInt() else 100
                            Text(
                                text = "$ratio%",
                                fontWeight = FontWeight.Bold,
                                color = if (ratio > 100) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                            )
                        }

                        LinearProgressIndicator(
                            progress = {
                                if (safeDailyAllowance > 0) min(1f, (dailyBurnRate / safeDailyAllowance).toFloat()) else 1f
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (dailyBurnRate > safeDailyAllowance) MaterialTheme.colorScheme.error else Color(0xFF00C853),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Key Breakdown Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Liquid Cash Pool", style = MaterialTheme.typography.bodyMedium)
                        Text(CurrencyUtils.formatCurrency(totalLiquidBalance, baseCurrency), fontWeight = FontWeight.Bold)
                    }

                    if (remainingMonthRecurringIncome > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Recurring Inflows (Allowance/Salary)", style = MaterialTheme.typography.bodyMedium)
                            Text("+${CurrencyUtils.formatCurrency(remainingMonthRecurringIncome, baseCurrency)}", fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                        }
                    }

                    if (remainingMonthRecurringExpense > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Bills & Subscriptions", style = MaterialTheme.typography.bodyMedium)
                            Text("-${CurrencyUtils.formatCurrency(remainingMonthRecurringExpense, baseCurrency)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (totalExpectedFreelanceReceivable > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expected Freelance Payouts (Milestones)", style = MaterialTheme.typography.bodyMedium)
                            Text("+${CurrencyUtils.formatCurrency(totalExpectedFreelanceReceivable, baseCurrency)}", fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                        }
                    }

                    if (totalExpectedDebtReceivable > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expected Payouts (Lent to others)", style = MaterialTheme.typography.bodyMedium)
                            Text("+${CurrencyUtils.formatCurrency(totalExpectedDebtReceivable, baseCurrency)}", fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                        }
                    }

                    if (totalExpectedDebtPayable > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expected Repayments (Borrowed)", style = MaterialTheme.typography.bodyMedium)
                            Text("-${CurrencyUtils.formatCurrency(totalExpectedDebtPayable, baseCurrency)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unallocated Discretionary Fund", style = MaterialTheme.typography.bodyMedium)
                        val disc = max(0.0, totalLiquidBalance + remainingMonthRecurringIncome - remainingMonthRecurringExpense - totalExpectedDebtPayable)
                        Text(CurrencyUtils.formatCurrency(disc, baseCurrency), fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Month-over-Month Category Variance & Trends
 */
@Composable
private fun CategoryVarianceTab(
    currentMonthTxs: List<TransactionEntity>,
    prevMonthTxs: List<TransactionEntity>,
    allCategories: List<CategoryEntity>,
    baseCurrency: String,
    currentMonthLabel: String
) {
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    val currentCatTotals = remember(currentMonthTxs) {
        currentMonthTxs
            .filter { it.type == TransactionType.EXPENSE.displayName && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    val prevCatTotals = remember(prevMonthTxs) {
        prevMonthTxs
            .filter { it.type == TransactionType.EXPENSE.displayName && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    val allCatIds = remember(currentCatTotals, prevCatTotals) {
        (currentCatTotals.keys + prevCatTotals.keys).distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Month-over-Month Spending Shifts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Comparing expense categories in $currentMonthLabel vs previous month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (allCatIds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No category expense history available for comparison.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(allCatIds.sortedByDescending { currentCatTotals[it] ?: 0.0 }) { catId ->
                val category = categoryMap[catId]
                val currentSpend = currentCatTotals[catId] ?: 0.0
                val prevSpend = prevCatTotals[catId] ?: 0.0

                val deltaAmount = currentSpend - prevSpend
                val percentDelta = if (prevSpend > 0) {
                    ((currentSpend - prevSpend) / prevSpend * 100)
                } else if (currentSpend > 0) 100.0 else 0.0

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category?.name ?: "General Expense",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Prev: ${CurrencyUtils.formatCurrency(prevSpend, baseCurrency)} → Now: ${CurrencyUtils.formatCurrency(currentSpend, baseCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (deltaAmount > 0) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (deltaAmount > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (deltaAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = (if (deltaAmount > 0) "+" else "") + String.format(Locale.getDefault(), "%.1f%%", percentDelta),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (deltaAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
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

/**
 * Tab 4: 100% Offline Algorithmic Financial Health Score (0–100)
 */
@Composable
private fun FinancialHealthScoreTab(
    allAccounts: List<AccountEntity>,
    allTransactions: List<TransactionEntity>,
    monthIncome: Double,
    monthExpense: Double,
    allBudgets: List<BudgetEntity>,
    allDebts: List<DebtEntity>,
    baseCurrency: String
) {
    // 1. Savings Rate Score (0 - 30 pts)
    val savingsRate = if (monthIncome > 0) max(0.0, (monthIncome - monthExpense) / monthIncome * 100) else 0.0
    val savingsScore = when {
        savingsRate >= 30.0 -> 30
        savingsRate >= 20.0 -> 24
        savingsRate >= 10.0 -> 16
        savingsRate > 0.0 -> 8
        else -> 0
    }

    // 2. Budget Discipline Score (0 - 25 pts)
    val budgetScore = remember(allBudgets, allTransactions) {
        if (allBudgets.isEmpty()) 20 // Default reasonable points if no budgets set
        else {
            val adhered = allBudgets.count { budget ->
                val spent = allTransactions
                    .filter { it.categoryId == budget.categoryId && it.type == TransactionType.EXPENSE.displayName }
                    .sumOf { it.amount }
                spent <= budget.amountLimit
            }
            ((adhered.toDouble() / allBudgets.size) * 25).roundToInt()
        }
    }

    // 3. Emergency Buffer Score (0 - 25 pts)
    val liquidCash = allAccounts.filter { it.includeInTally && !it.isLocked }.sumOf { it.currentBalance }
    val monthsRunway = if (monthExpense > 0) liquidCash / monthExpense else 6.0
    val emergencyScore = when {
        monthsRunway >= 6.0 -> 25
        monthsRunway >= 3.0 -> 18
        monthsRunway >= 1.0 -> 10
        else -> 4
    }

    // 4. Debt Burden Score (0 - 20 pts)
    val activeDebts = allDebts.filter { it.type.equals("Borrowed", ignoreCase = true) && it.status.equals("Active", ignoreCase = true) }.sumOf { it.remainingAmount }
    val totalNetWorth = allAccounts.filter { it.includeInTally }.sumOf { it.currentBalance }
    val debtRatio = if (totalNetWorth > 0.0) (activeDebts / totalNetWorth * 100.0) else if (activeDebts > 0.0) 100.0 else 0.0
    val debtScore = when {
        activeDebts == 0.0 -> 20
        debtRatio <= 15.0 -> 17
        debtRatio <= 30.0 -> 12
        debtRatio <= 50.0 -> 6
        else -> 2
    }

    val totalScore = min(100, savingsScore + budgetScore + emergencyScore + debtScore)

    val (scoreGrade, scoreColor) = when {
        totalScore >= 85 -> "Excellent" to Color(0xFF00C853)
        totalScore >= 70 -> "Good" to Color(0xFF43A047)
        totalScore >= 50 -> "Fair" to Color(0xFFFFA000)
        else -> "Needs Attention" to Color(0xFFE53935)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Score Radial Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = scoreColor.copy(alpha = 0.12f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(scoreColor, scoreColor.copy(alpha = 0.5f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalScore",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Financial Health: $scoreGrade",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Calculated 100% offline from your local savings rate, emergency buffer, budget compliance, and debt ratio.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Four Key Pillar Bars
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Health Pillars Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Savings Rate
                    HealthPillarRow(
                        title = "Savings Rate (${String.format(Locale.getDefault(), "%.1f%%", savingsRate)})",
                        score = savingsScore,
                        maxScore = 30,
                        color = Color(0xFF00C853)
                    )

                    // 2. Budget Discipline
                    HealthPillarRow(
                        title = "Budget Compliance",
                        score = budgetScore,
                        maxScore = 25,
                        color = Color(0xFF2196F3)
                    )

                    // 3. Emergency Runway
                    HealthPillarRow(
                        title = "Emergency Runway (${String.format(Locale.getDefault(), "%.1f", monthsRunway)} mos)",
                        score = emergencyScore,
                        maxScore = 25,
                        color = Color(0xFFFF9800)
                    )

                    // 4. Debt-to-Asset
                    HealthPillarRow(
                        title = "Debt Burden Ratio (${String.format(Locale.getDefault(), "%.1f%%", debtRatio)})",
                        score = debtScore,
                        maxScore = 20,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }

        // Smart Offline Tips
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFB300))
                        Text(
                            text = "Smart Financial Recommendations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (savingsRate < 20.0) {
                        Text(
                            text = "• Aim to save at least 20% of monthly income to build long-term compounding growth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (monthsRunway < 3.0) {
                        Text(
                            text = "• Your emergency fund is under 3 months. Prioritize building liquid savings in your cash accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeDebts > 0.0) {
                        Text(
                            text = "• Focus on clearing active liabilities (${CurrencyUtils.formatCurrency(activeDebts, baseCurrency)}) to boost your debt score.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (allBudgets.isEmpty()) {
                        Text(
                            text = "• Set monthly category budget limits in the Budgets tab to track and curb overspending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthPillarRow(
    title: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("$score / $maxScore pts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { score.toFloat() / maxScore },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
