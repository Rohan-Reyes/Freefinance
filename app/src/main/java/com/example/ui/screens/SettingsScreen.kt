package com.example.ui.screens

import com.example.ui.theme.FinPositive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.AppThemePreset
import com.example.data.preferences.HeroBackgroundStyle
import com.example.util.BiometricAuthHelper
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardEvent
import com.example.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsSectionFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Rounded.Tune),
    APPEARANCE("Appearance", Icons.Rounded.Palette),
    SECURITY("Security", Icons.Rounded.Fingerprint),
    CURRENCY("Currency", Icons.Rounded.CurrencyExchange),
    DATA_SYNC("Data & sync", Icons.Rounded.CloudSync),
    SYSTEM("About", Icons.Rounded.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWidgets: () -> Unit = {},
    onNavigateToApkReleases: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToCustomize: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToCards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cloudStatus by viewModel.cloudBackupStatus.collectAsState()
    val heroBackgroundStyle by viewModel.heroBackgroundStyle.collectAsState()
    val appThemePreset by viewModel.appThemePreset.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val hideMoney by viewModel.hideMoney.collectAsState()
    val includeAssetsInHero by viewModel.includeAssetsInHero.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val requireBiometricForCards by viewModel.requireBiometricForCards.collectAsState()
    val appLockPin by viewModel.appLockPin.collectAsState()
    val biometricTimeoutMode by viewModel.biometricTimeoutMode.collectAsState()

    var selectedFilter by remember { mutableStateOf(SettingsSectionFilter.ALL) }

    // Dialog & Confirmation states
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var pendingJsonImportUri by remember { mutableStateOf<Uri?>(null) }
    var showDriveRestoreDialog by remember { mutableStateOf(false) }
    var showAddExchangeRateDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }

    // Currency selector menu
    var baseCurrencyMenuExpanded by remember { mutableStateOf(false) }

    // Activity Result Launchers
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDataToCsv(context, uri)
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportStateToJson(context, uri)
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingJsonImportUri = uri
            showImportJsonDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is DashboardEvent.ExportCompleted -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is DashboardEvent.SyncCompleted -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is DashboardEvent.CurrencyUpdated -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is DashboardEvent.Error -> {
                    Toast.makeText(context, event.error, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    val showSecurity = selectedFilter == SettingsSectionFilter.ALL || selectedFilter == SettingsSectionFilter.SECURITY
    val showAppearance = selectedFilter == SettingsSectionFilter.ALL || selectedFilter == SettingsSectionFilter.APPEARANCE
    val showCurrency = selectedFilter == SettingsSectionFilter.ALL || selectedFilter == SettingsSectionFilter.CURRENCY
    val showDataSync = selectedFilter == SettingsSectionFilter.ALL || selectedFilter == SettingsSectionFilter.DATA_SYNC
    val showSystem = selectedFilter == SettingsSectionFilter.ALL || selectedFilter == SettingsSectionFilter.SYSTEM

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleHideMoney(!hideMoney) },
                        modifier = Modifier.testTag("settings_top_toggle_privacy")
                    ) {
                        Icon(
                            imageVector = if (hideMoney) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideMoney) "Show Money" else "Hide Money",
                            tint = if (hideMoney) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -------------------------------------------------------------
            // CATEGORY FILTER BAR
            // -------------------------------------------------------------
            item(key = "SETTINGS_CATEGORY_FILTER") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SettingsSectionFilter.entries) { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // =============================================================
            // 1. APPEARANCE & PERSONALIZATION STUDIO
            // =============================================================
            if (showAppearance) {
                item(key = "HEADER_APPEARANCE") {
                    SettingsSectionHeader(
                        title = "Appearance",
                        subtitle = "Theme, colours and dashboard layout"
                    )
                }

                // Theme: mode, palette and hero style in one compact card
                item(key = "CARD_THEME_PRESETS") {
                    SettingsGroupCard {
                        Text("Theme mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AppThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = appThemeMode == mode,
                                    onClick = { viewModel.setAppThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index, AppThemeMode.entries.size)
                                ) {
                                    Text(when (mode) { AppThemeMode.SYSTEM -> "System"; AppThemeMode.DARK -> "Dark"; AppThemeMode.LIGHT -> "Light" })
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text("Colour palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(appThemePreset.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AppThemePreset.entries.forEach { preset ->
                                val (c1, c2) = paletteSwatch(preset)
                                SwatchOption(
                                    label = preset.title.substringBefore(' ').ifBlank { preset.title },
                                    brush = Brush.linearGradient(listOf(c1, c2)),
                                    selected = appThemePreset == preset,
                                    onClick = { viewModel.setAppThemePreset(preset, syncHeroStyle = true) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text("Balance card style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HeroBackgroundStyle.entries.forEach { style ->
                                SwatchOption(
                                    label = style.title.substringBefore(' '),
                                    brush = heroSwatch(style, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary),
                                    selected = heroBackgroundStyle == style,
                                    onClick = { viewModel.setHeroBackgroundStyle(style) }
                                )
                            }
                        }
                    }
                }

                // Privacy, Net Worth Inclusion & Layout Customization
                item(key = "CARD_DISPLAY_CONTROLS") {
                    SettingsGroupCard {
                        // 1. Hide My Money Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                SettingsIconBadge(
                                    icon = if (hideMoney) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    containerColor = if (hideMoney) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (hideMoney) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Privacy Mask (Hide Money)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hideMoney) "Balances masked with asterisks (***)" else "Exact balances visible on screen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = hideMoney,
                                onCheckedChange = { viewModel.toggleHideMoney(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 2. Include Assets in Hero Net Worth Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                SettingsIconBadge(
                                    icon = Icons.Default.Inventory2,
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = "Include Assets in Hero Total",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (includeAssetsInHero) "Combining liquid cash + tracked valuables" else "Showing liquid account balances only",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = includeAssetsInHero,
                                onCheckedChange = { viewModel.toggleIncludeAssetsInHero(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 3. Customize Dashboard Cards Action Row
                        SettingsNavigationRow(
                            icon = Icons.Default.Tune,
                            title = "Customize Dashboard Cards",
                            subtitle = "Reorder, enable, or hide modular analytics cards",
                            onClick = onNavigateToCustomize
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 4. Category Manager Action Row
                        SettingsNavigationRow(
                            icon = Icons.Default.Category,
                            title = "Category & Tag Manager",
                            subtitle = "Configure custom income & expense categories",
                            onClick = onNavigateToCategories
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 5. Home Screen Widgets Action Row
                        SettingsNavigationRow(
                            icon = Icons.Default.Widgets,
                            title = "Home Screen Widgets",
                            subtitle = "Pin live net worth & 1-tap quick log widgets",
                            onClick = onNavigateToWidgets
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 6. Cards & Credit Limits Vault Action Row
                        SettingsNavigationRow(
                            icon = Icons.Default.CreditCard,
                            title = "Cards & Credit Limits Vault",
                            subtitle = "Manage linked Debit cards & track Credit card limits",
                            onClick = onNavigateToCards
                        )
                    }
                }
            }

            // =============================================================
            // 1B. BIOMETRICS & VAULT SECURITY
            // =============================================================
            if (showSecurity) {
                item(key = "HEADER_SECURITY") {
                    SettingsSectionHeader(
                        title = "Security",
                        subtitle = "App lock, biometrics and PIN"
                    )
                }

                item(key = "CARD_BIOMETRIC_SECURITY") {
                    val hardwareStatus = remember(context) { BiometricAuthHelper.getHardwareStatus(context) }

                    SettingsGroupCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                SettingsIconBadge(
                                    icon = Icons.Default.Fingerprint,
                                    containerColor = if (biometricLockEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (biometricLockEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Biometrics to Open App",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = hardwareStatus.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = biometricLockEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setBiometricLockEnabled(enabled)
                                    if (enabled && appLockPin.isBlank()) {
                                        showSetPinDialog = true
                                    }
                                },
                                modifier = Modifier.testTag("switch_biometric_app_lock")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Require Biometric to Reveal Card Numbers / CVV
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                SettingsIconBadge(
                                    icon = Icons.Default.CreditCard,
                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                )
                                Column {
                                    Text(
                                        text = "Biometric Lock for Card Details",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Require biometric verify before revealing full card number & CVV",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = requireBiometricForCards,
                                onCheckedChange = { viewModel.setRequireBiometricForCards(it) },
                                modifier = Modifier.testTag("switch_biometric_card_reveal")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Auto-Lock Timeout Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Auto-Lock When App Leaves Screen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "IMMEDIATE" to "Immediately",
                                    "AFTER_1_MIN" to "After 1 Min",
                                    "AFTER_5_MIN" to "After 5 Mins"
                                ).forEach { (modeKey, modeLabel) ->
                                    FilterChip(
                                        selected = biometricTimeoutMode == modeKey,
                                        onClick = { viewModel.setBiometricTimeoutMode(modeKey) },
                                        label = {
                                            Text(
                                                text = modeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Fallback PIN & Test Lock Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSetPinDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_configure_app_pin")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (appLockPin.isBlank()) "Set 4-Digit PIN" else "Change PIN (••••)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick = {
                                    if (!biometricLockEnabled) {
                                        viewModel.setBiometricLockEnabled(true)
                                    }
                                    viewModel.lockAppNow()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_lock_app_now")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lock App Now",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // =============================================================
            // 2. MULTI-CURRENCY ENGINE & CUSTOM EXCHANGE RATES
            // =============================================================
            if (showCurrency) {
                item(key = "HEADER_CURRENCY") {
                    SettingsSectionHeader(
                        title = "Currency",
                        subtitle = "Base currency and exchange rates"
                    )
                }

                item(key = "CARD_CURRENCY_ENGINE") {
                    SettingsGroupCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsIconBadge(
                                icon = Icons.Default.CurrencyExchange,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Base Currency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All foreign accounts & assets convert into this currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = baseCurrencyMenuExpanded,
                            onExpandedChange = { baseCurrencyMenuExpanded = !baseCurrencyMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val activeBase = CurrencyUtils.SUPPORTED_CURRENCIES.find { it.code == uiState.baseCurrency }
                                ?: CurrencyUtils.SUPPORTED_CURRENCIES.first()

                            OutlinedTextField(
                                value = "${activeBase.code} (${activeBase.symbol}) — ${activeBase.name}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Primary Reporting Currency") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseCurrencyMenuExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = baseCurrencyMenuExpanded,
                                onDismissRequest = { baseCurrencyMenuExpanded = false }
                            ) {
                                CurrencyUtils.SUPPORTED_CURRENCIES.forEach { curr ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${curr.code} (${curr.symbol})", fontWeight = FontWeight.Bold)
                                                Text(curr.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setBaseCurrency(curr.code)
                                            baseCurrencyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Custom Exchange Rates Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Exchange Rate Overrides",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Override default international conversion ratios",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                onClick = { showAddExchangeRateDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Add Rate",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (uiState.exchangeRates.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No custom FX rates configured. Using built-in international conversion ratios.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.exchangeRates.forEach { rateEntity ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
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
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "${rateEntity.fromCurrency} → ${rateEntity.toCurrency}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "1 ${rateEntity.fromCurrency} = ${rateEntity.rate} ${rateEntity.toCurrency}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteExchangeRate(rateEntity.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Rate",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
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

            // =============================================================
            // 3. DATA VAULT, CLOUD SYNC & LEDGER INTEGRITY
            // =============================================================
            if (showDataSync) {
                item(key = "HEADER_DATA_SYNC") {
                    SettingsSectionHeader(
                        title = "Data & sync",
                        subtitle = "Server sync, backups and exports"
                    )
                }

                // Server sync (FreeFinance sync server / web app)
                item(key = "CARD_SERVER_SYNC") {
                    com.example.ui.components.ServerSyncCard()
                }

                // Balance Integrity & Calibration Card
                item(key = "CARD_BALANCE_INTEGRITY") {
                    val negativeAccountsCount = uiState.accounts.count { it.currentBalance < 0.0 }
                    SettingsGroupCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsIconBadge(
                                icon = if (negativeAccountsCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                containerColor = if (negativeAccountsCount > 0) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    FinPositive.copy(alpha = 0.16f)
                                },
                                contentColor = if (negativeAccountsCount > 0) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    FinPositive
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Balance Integrity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (negativeAccountsCount > 0) {
                                        "$negativeAccountsCount account(s) currently show a negative balance. Open the account to calibrate or run safe reconciliation."
                                    } else {
                                        "All ${uiState.accounts.size} accounts & sub-accounts are synchronized with cleared transactions."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.reconcileBalancesManually() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_verify_balances_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify & Reconcile Account Balances", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Google Drive Cloud Backup Card
                item(key = "CARD_CLOUD_BACKUP") {
                    SettingsGroupCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsIconBadge(
                                icon = Icons.Default.CloudUpload,
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive Encrypted Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Private backup snapshot stored in your Google Drive AppData",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Account Connection Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (cloudStatus.userEmail != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Column {
                                        Text(
                                            text = cloudStatus.userEmail ?: "Not Connected",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (cloudStatus.userEmail != null) "Google Drive AppData Ready" else "Sign in to enable cloud backup",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (cloudStatus.userEmail == null) {
                                    Button(
                                        onClick = { viewModel.authenticateWithGoogle(context) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (cloudStatus.isBackingUp || cloudStatus.isRestoring) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    text = if (cloudStatus.isBackingUp) "Uploading encrypted snapshot to Drive..." else "Restoring snapshot from Drive...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (cloudStatus.lastBackupTime != null) {
                            val timeStr = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(cloudStatus.lastBackupTime ?: 0L))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = FinPositive, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Last cloud sync: $timeStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.backupToCloudDrive(context) },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !cloudStatus.isBackingUp && !cloudStatus.isRestoring,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { showDriveRestoreDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !cloudStatus.isBackingUp && !cloudStatus.isRestoring,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Universal JSON State & CSV Spreadsheet Export Card
                item(key = "CARD_LOCAL_EXPORT_IMPORT") {
                    SettingsGroupCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsIconBadge(
                                icon = Icons.Default.Sync,
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Snapshots & Ledger Export",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Lossless JSON state snapshots & spreadsheet-ready CSV files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    jsonExportLauncher.launch("finance_state_snapshot_$timestamp.json")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    jsonImportLauncher.launch("application/json")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                csvExportLauncher.launch("finance_tracker_export_$timestamp.csv")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Spreadsheet Ledger (CSV)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // =============================================================
            // 4. SYSTEM DIAGNOSTICS, RELEASES & CREATOR SUPPORT
            // =============================================================
            if (showSystem) {
                item(key = "HEADER_SYSTEM") {
                    SettingsSectionHeader(
                        title = "About",
                        subtitle = "Version, analytics and release notes"
                    )
                }

                // 2x2 Bento Local Engine Metrics Grid
                item(key = "CARD_ENGINE_METRICS") {
                    SettingsGroupCard {
                        SettingsNavigationRow(
                            icon = Icons.Rounded.Insights,
                            title = "Analytics & forecast",
                            subtitle = "Health score, spending heatmap and burn rate",
                            onClick = onNavigateToAnalytics
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsNavigationRow(
                            icon = Icons.Rounded.NewReleases,
                            title = "Release notes",
                            subtitle = "Version 1.2.0",
                            onClick = onNavigateToApkReleases
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsNavigationRow(
                            icon = Icons.Rounded.LocalCafe,
                            title = "Support development",
                            subtitle = "buymeacoffee.com/rohan_reyes",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rohan_reyes")))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                item(key = "CARD_SUPPORT_AND_SECURITY") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Your data is stored on this phone. It only leaves the device when you use server sync, Google Drive backup or an export.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOG: SET / CHANGE 4-DIGIT SECURITY PIN
    // -------------------------------------------------------------
    if (showSetPinDialog) {
        var newPinInput by remember { mutableStateOf(appLockPin) }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Configure 4-Digit Security PIN", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your 4-digit PIN acts as a fallback alongside Fingerprint / Face Unlock when opening the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { input ->
                            newPinInput = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("4-Digit Security PIN") },
                        placeholder = { Text("e.g. 1234") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_security_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length == 4) {
                            viewModel.setAppLockPin(newPinInput)
                            Toast.makeText(context, "4-Digit Security PIN saved", Toast.LENGTH_SHORT).show()
                            showSetPinDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DIALOG: CONFIRM JSON IMPORT
    // -------------------------------------------------------------
    if (showImportJsonDialog && pendingJsonImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportJsonDialog = false
                pendingJsonImportUri = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Replace Local State with JSON?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Importing a JSON state snapshot will completely replace all existing accounts, transactions, subscriptions, budgets, debts, and projects with the data from the imported file.\n\nThis action cannot be undone. Are you sure you want to proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingJsonImportUri
                        showImportJsonDialog = false
                        pendingJsonImportUri = null
                        if (uri != null) {
                            viewModel.importStateFromJson(context, uri)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportJsonDialog = false
                        pendingJsonImportUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DIALOG: CONFIRM CLOUD DRIVE RESTORE
    // -------------------------------------------------------------
    if (showDriveRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showDriveRestoreDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Database from Google Drive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will download the latest backup snapshot from your Google Drive AppData folder and overwrite your current local database.\n\nDo you want to proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveRestoreDialog = false
                        viewModel.restoreFromCloudDrive(context)
                    }
                ) {
                    Text("Restore Database", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DIALOG: ADD / UPDATE EXCHANGE RATE
    // -------------------------------------------------------------
    if (showAddExchangeRateDialog) {
        var fromCurrency by remember { mutableStateOf("EUR") }
        var toCurrency by remember { mutableStateOf(uiState.baseCurrency) }
        var rateText by remember { mutableStateOf("1.08") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddExchangeRateDialog = false },
            title = { Text("Add / Update Exchange Rate", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fromCurrency,
                        onValueChange = { fromCurrency = it.uppercase().trim() },
                        label = { Text("From Currency (e.g., EUR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = toCurrency,
                        onValueChange = { toCurrency = it.uppercase().trim() },
                        label = { Text("To Currency (e.g., USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate Multiplier (1 $fromCurrency = ? $toCurrency)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error != null) {
                        Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = rateText.toDoubleOrNull()
                        if (fromCurrency.length != 3 || toCurrency.length != 3) {
                            error = "Currency codes must be 3 letters (e.g. EUR, USD)"
                            return@Button
                        }
                        if (rate == null || rate <= 0) {
                            error = "Please enter a valid positive exchange rate"
                            return@Button
                        }
                        viewModel.addOrUpdateExchangeRate(fromCurrency, toCurrency, rate)
                        showAddExchangeRateDialog = false
                    }
                ) {
                    Text("Save Rate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExchangeRateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsLivePreviewHeroCard(
    totalBalance: Double,
    baseCurrency: String,
    accountCount: Int,
    appThemePreset: AppThemePreset,
    heroBackgroundStyle: HeroBackgroundStyle,
    appThemeMode: AppThemeMode,
    hideMoney: Boolean,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onToggleHideMoney: () -> Unit
) {
    val isGradient = heroBackgroundStyle != HeroBackgroundStyle.SOLID
    val gradientBrush = remember(heroBackgroundStyle) {
        when (heroBackgroundStyle) {
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

    val contentColor = if (isGradient) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradient) Color.Transparent else MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isGradient) {
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(gradientBrush)
                } else Modifier
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.9f, size.height * 0.15f)
                )
            }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isGradient) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Live theme & hero preview",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${appThemePreset.title} • ${heroBackgroundStyle.title}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Surface(
                    onClick = onToggleHideMoney,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isGradient) Color.Black.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (hideMoney) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isGradient) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (hideMoney) "Masked" else baseCurrency,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGradient) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = CurrencyUtils.formatCurrency(totalBalance, baseCurrency, hideMoney),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                    Text(
                        text = "$accountCount connected account${if (accountCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            // Segmented Theme Mode Selector (System / Dark / Light)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGradient) Color.Black.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = appThemeMode == mode
                        val modeIcon = when (mode) {
                            AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            AppThemeMode.DARK -> Icons.Default.DarkMode
                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                        }
                        Surface(
                            onClick = { onThemeModeSelected(mode) },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) {
                                if (isGradient) Color.White else MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    tint = if (selected) {
                                        if (isGradient) Color(0xFF1A1C1E) else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        contentColor.copy(alpha = 0.85f)
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (selected) {
                                        if (isGradient) Color(0xFF1A1C1E) else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        contentColor.copy(alpha = 0.85f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsGroupCard(
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

/** Round colour swatch with a label underneath; used for palette and balance-card style. */
@Composable
private fun SwatchOption(label: String, brush: Brush, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(brush)
                .then(
                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private fun paletteSwatch(preset: AppThemePreset): Pair<Color, Color> = when (preset) {
    AppThemePreset.MATERIAL_YOU -> Color(0xFF6750A4) to Color(0xFF00B4DB)
    AppThemePreset.EMERALD_AURORA -> Color(0xFF0F8A6F) to Color(0xFF2CC59A)
    AppThemePreset.OCEAN_INDIGO -> Color(0xFF1E5BB0) to Color(0xFF4FA3FF)
    AppThemePreset.SUNSET_GLOW -> Color(0xFFBA1A3E) to Color(0xFFF27121)
    AppThemePreset.MIDNIGHT_NEBULA -> Color(0xFF5B3A8E) to Color(0xFFB98AF0)
    AppThemePreset.GOLDEN_LUXE -> Color(0xFF8B5000) to Color(0xFFFFB95F)
    AppThemePreset.CYBERPUNK_NEON -> Color(0xFF006874) to Color(0xFFFF4FD8)
    AppThemePreset.TITANIUM_SLATE -> Color(0xFF334155) to Color(0xFF94A3B8)
}

private fun heroSwatch(style: HeroBackgroundStyle, container: Color, primary: Color): Brush = when (style) {
    HeroBackgroundStyle.SOLID -> Brush.linearGradient(listOf(container, primary.copy(alpha = 0.6f)))
    HeroBackgroundStyle.GRADIENT -> Brush.linearGradient(listOf(Color(0xFF1E3C72), Color(0xFF0072FF)))
    HeroBackgroundStyle.SUNSET_GLOW -> Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
    HeroBackgroundStyle.EMERALD_AURORA -> Brush.linearGradient(listOf(Color(0xFF0575E6), Color(0xFF00F260)))
    HeroBackgroundStyle.MIDNIGHT_NEBULA -> Brush.linearGradient(listOf(Color(0xFF200122), Color(0xFF9013FE)))
    HeroBackgroundStyle.GOLDEN_LUXE -> Brush.linearGradient(listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B)))
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            SettingsIconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsMetricTile(
    label: String,
    value: String,
    sublabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
