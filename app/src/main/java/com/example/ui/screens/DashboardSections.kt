package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.ui.brand.BrandMark
import com.example.ui.brand.Brands
import com.example.ui.brand.InstitutionLogo
import com.example.ui.components.ds.IconBadge
import com.example.ui.components.ds.RowDivider
import com.example.ui.components.ds.SectionCard
import com.example.ui.components.ds.SectionHeader
import com.example.ui.components.ds.StatusPill
import com.example.ui.theme.FinNegative
import com.example.ui.theme.FinPositive
import com.example.util.BankAppLauncher
import com.example.util.CurrencyUtils
import java.util.Locale

/** One icon per account type, used wherever an account has no recognised brand. */
fun accountTypeIcon(type: String): ImageVector = when (type.lowercase(Locale.ROOT)) {
    "cash" -> Icons.Rounded.Payments
    "bank account", "bank" -> Icons.Rounded.AccountBalance
    "time deposit", "savings", "locked deposit / investment" -> Icons.Rounded.Savings
    "e-wallet", "wallet" -> Icons.Rounded.Smartphone
    "investment" -> Icons.Rounded.ShowChart
    else -> Icons.Rounded.AccountBalanceWallet
}

/** Brand mark for recognised banks / e-wallets, otherwise a tonal badge in the account's colour. */
@Composable
fun AccountSymbol(account: AccountEntity, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val brand = remember(account.name, account.linkedAppPackage) { Brands.detect(account.name, account.linkedAppPackage) }
    InstitutionLogo(name = account.name, explicitPackage = account.linkedAppPackage, size = size) {
        if (brand != null) {
            BrandMark(brand, size = size)
        } else {
            val tint = Color(account.color)
            IconBadge(icon = accountTypeIcon(account.type), size = size, tint = tint, container = tint.copy(alpha = 0.16f))
        }
    }
}

/**
 * Accounts & sub-accounts as one clean list: brand/type symbol, name, type · currency, balance.
 * Sub-accounts are indented under their parent.
 */
@Composable
fun AccountsBreakdownRow(
    accounts: List<AccountEntity>,
    cardsCount: Int = 0,
    hideMoney: Boolean = false,
    onAddAccountClick: () -> Unit,
    onAddSubAccountClick: (Long) -> Unit,
    onAccountClick: (Long) -> Unit,
    onNavigateToCards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val roots = remember(accounts) {
        val ids = accounts.map { it.id }.toSet()
        // Orphan sub-accounts (parent deleted) are shown as top-level rows.
        accounts.filter { it.parentAccountId == null || it.parentAccountId !in ids }
    }
    val children = remember(accounts) { accounts.filter { it.parentAccountId != null }.groupBy { it.parentAccountId!! } }

    SectionCard(modifier = modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
        SectionHeader(
            title = "Accounts",
            subtitle = if (accounts.isEmpty()) "Cash, banks and e-wallets" else "${accounts.size} accounts",
            actionLabel = "Add",
            onAction = onAddAccountClick
        )
        Spacer(Modifier.height(4.dp))
        if (accounts.isEmpty()) {
            Text(
                "Add your cash, bank and e-wallet accounts to start tracking balances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        roots.forEachIndexed { index, account ->
            if (index > 0) RowDivider(inset = 52.dp)
            AccountListRow(
                account = account,
                hideMoney = hideMoney,
                onClick = { onAccountClick(account.id) },
                onAddSubAccount = if (account.parentAccountId == null) ({ onAddSubAccountClick(account.id) }) else null
            )
            children[account.id].orEmpty().forEach { sub ->
                SubAccountListRow(sub, hideMoney, onClick = { onAccountClick(sub.id) })
            }
        }
    }
}

@Composable
private fun AccountListRow(
    account: AccountEntity,
    hideMoney: Boolean,
    onClick: () -> Unit,
    onAddSubAccount: (() -> Unit)?
) {
    val context = LocalContext.current
    val hasApp = remember(account.name, account.linkedAppPackage) {
        BankAppLauncher.findMatchingBankApp(account.name, account.linkedAppPackage) != null || !account.linkedAppPackage.isNullOrBlank()
    }
    val balance = remember(account.currentBalance, account.currencyCode, hideMoney) {
        CurrencyUtils.formatCurrency(account.currentBalance, account.currencyCode, hideMoney)
    }
    Surface(onClick = onClick, color = Color.Transparent, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().testTag("account_card_${account.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountSymbol(account)
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${account.subAccountType ?: account.type} · ${account.currencyCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (!account.includeInTally) StatusPill("Excluded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (account.isLocked || account.lockedUntil != null) {
                        Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Text(
                balance,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (account.currentBalance < 0) FinNegative else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (hasApp) {
                IconButton(onClick = { BankAppLauncher.launchBankApp(context, account.name, account.linkedAppPackage) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open ${account.name} app", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            if (onAddSubAccount != null) {
                IconButton(onClick = onAddSubAccount, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add sub-account to ${account.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            } else if (!hasApp) {
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun SubAccountListRow(sub: AccountEntity, hideMoney: Boolean, onClick: () -> Unit) {
    val balance = remember(sub.currentBalance, sub.currencyCode, hideMoney) {
        CurrencyUtils.formatCurrency(sub.currentBalance, sub.currencyCode, hideMoney)
    }
    Surface(onClick = onClick, color = Color.Transparent, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().testTag("sub_account_row_${sub.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).padding(0.dp)) {
                Surface(shape = CircleShape, color = Color(sub.color), modifier = Modifier.size(8.dp)) {}
            }
            Text(
                sub.name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }.takeIf { sub.name.all { c -> !c.isLetter() || c.isUpperCase() } } ?: sub.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (sub.interestRatePa > 0f) StatusPill(String.format(Locale.getDefault(), "%.1f%%", sub.interestRatePa), color = FinPositive)
            if (sub.isLocked || sub.lockedUntil != null) {
                Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
            }
            Text(balance, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Kept for other screens that show a single account tile. */
@Composable
fun CompactAccountCard(
    account: AccountEntity,
    hideMoney: Boolean = false,
    onAddSubAccount: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
        AccountListRow(account, hideMoney, onClick, onAddSubAccount)
    }
}

private data class NavItem(val label: String, val icon: ImageVector, val onClick: () -> Unit, val badge: Int = 0, val tag: String? = null)

/** Shortcut grid: ten equal tiles (icon + label), two rows of five. */
@Composable
internal fun DashboardNavigationGrid(
    onNavigateToReceiptInbox: () -> Unit,
    pendingReceiptCount: Int,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToCommission: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCards: () -> Unit = {},
    cardsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Records", Icons.AutoMirrored.Rounded.ReceiptLong, onNavigateToRecords),
        NavItem("Analytics", Icons.AutoMirrored.Rounded.TrendingUp, onNavigateToAnalytics),
        NavItem("Cards", Icons.Rounded.CreditCard, onNavigateToCards, tag = "dashboard_nav_cards_vault"),
        NavItem("Budgets", Icons.Rounded.PieChart, onNavigateToBudgets),
        NavItem("Bills", Icons.Rounded.EventRepeat, onNavigateToSubscriptions),
        NavItem("Receipts", Icons.Rounded.DocumentScanner, onNavigateToReceiptInbox, badge = pendingReceiptCount),
        NavItem("Debts", Icons.Rounded.Handshake, onNavigateToDebts),
        NavItem("Assets", Icons.Rounded.Inventory2, onNavigateToAssets),
        NavItem("Freelance", Icons.Rounded.BusinessCenter, onNavigateToCommission),
        NavItem("Categories", Icons.Rounded.Category, onNavigateToCategories)
    )
    SectionCard(modifier = modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
        items.chunked(5).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Surface(
                        onClick = item.onClick,
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f).then(if (item.tag != null) Modifier.testTag(item.tag) else Modifier)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (item.badge > 0) {
                                BadgedBox(badge = { Badge { Text("${item.badge}") } }) { IconBadge(item.icon, size = 44.dp) }
                            } else {
                                IconBadge(item.icon, size = 44.dp)
                            }
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
