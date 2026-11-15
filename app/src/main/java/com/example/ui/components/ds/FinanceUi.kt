package com.example.ui.components.ds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * My Finance design system — the small set of building blocks every screen should use so spacing,
 * shapes, colours and type stay consistent:
 *  - [SectionCard]   one panel style (surfaceContainerLow, large radius, hairline outline)
 *  - [SectionHeader] sentence-case title with an optional icon and a single text action
 *  - [IconBadge]     tonal rounded-square icon holder (replaces emoji "icons")
 *  - [ListRow]       48–64dp row with leading badge, title/subtitle and trailing content
 *  - [StatusPill]    small tinted label for states (Due, Locked, Virtual…)
 */
object FinanceDimens {
    val ScreenPadding: Dp = 16.dp
    val CardPadding: Dp = 16.dp
    val SectionGap: Dp = 12.dp
    val RowMinHeight: Dp = 56.dp
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(FinanceDimens.CardPadding),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, border = border, content = inner)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, border = border, content = inner)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) IconBadge(icon = icon, size = 28.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke(this)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = tint.copy(alpha = 0.14f),
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.32f),
        color = container
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = FinanceDimens.RowMinHeight,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
) {
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = Color.Transparent, content = body)
    } else {
        Box(modifier = modifier.fillMaxWidth()) { body() }
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
        }
    }
}

@Composable
fun RowDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    HorizontalDivider(
        modifier = modifier.padding(start = inset),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/** One overflow (⋮) menu per list item instead of rows of edit / delete icons. */
data class ItemAction(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

@Composable
fun ItemActionsMenu(actions: List<ItemAction>, modifier: Modifier = Modifier, contentDescription: String = "More actions") {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier) {
        androidx.compose.material3.IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(androidx.compose.material.icons.Icons.Rounded.MoreVert, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { a ->
                val tint = if (a.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(a.label, color = tint) },
                    leadingIcon = { Icon(a.icon, contentDescription = null, tint = tint) },
                    onClick = { open = false; a.onClick() }
                )
            }
        }
    }
}

/** Circle with a person's / item's initial — used for debts, bills and clients. */
@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(modifier = modifier.size(size), shape = androidx.compose.foundation.shape.CircleShape, color = color.copy(alpha = 0.16f)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().split(' ').filter { it.firstOrNull()?.isLetterOrDigit() == true }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "?" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
