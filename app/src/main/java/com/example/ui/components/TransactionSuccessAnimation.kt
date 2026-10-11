package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.viewmodels.DashboardViewModel
import com.example.viewmodels.TransactionCelebrationData
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val velocityX: Float,
    val velocityY: Float,
    val isStar: Boolean = false
)

/**
 * Celebratory full-screen animation overlay that triggers when an Income, Expense, or Transfer is logged.
 * Features flying confetti/sparkle particles, bouncy scale-in badge, and animated amount.
 */
@Composable
fun TransactionSuccessAnimationOverlay(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val celebrationData by viewModel.celebrationEvent.collectAsState()

    celebrationData?.let { data ->
        TransactionCelebrationCard(
            data = data,
            onDismiss = { viewModel.dismissCelebration() },
            modifier = modifier
        )
    }
}

@Composable
fun TransactionCelebrationCard(
    data: TransactionCelebrationData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    val isIncome = data.type == TransactionType.INCOME
    val isExpense = data.type == TransactionType.EXPENSE
    val isTransfer = data.type == TransactionType.TRANSFER

    val primaryColor = when {
        isIncome -> Color(0xFF00C853)
        isExpense -> Color(0xFFE53935)
        else -> Color(0xFF2979FF)
    }

    val secondaryColor = when {
        isIncome -> Color(0xFFFFD700)
        isExpense -> Color(0xFFFF7043)
        else -> Color(0xFF80D8FF)
    }

    // Generate celebratory particles
    val particles = remember(data.timestamp) {
        val count = if (isIncome) 32 else 20
        List(count) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val speed = Random.nextFloat() * 400f + 150f
            val colors = if (isIncome) {
                listOf(Color(0xFF00C853), Color(0xFFFFD700), Color(0xFF69F0AE), Color(0xFFB9F6CA), Color(0xFFFFEB3B))
            } else if (isExpense) {
                listOf(Color(0xFFE53935), Color(0xFFFF7043), Color(0xFFFFAB91), Color(0xFFFF5252))
            } else {
                listOf(Color(0xFF2979FF), Color(0xFF80D8FF), Color(0xFF40C4FF))
            }
            Particle(
                x = 0f,
                y = 0f,
                radius = Random.nextFloat() * 6f + 4f,
                color = colors[Random.nextInt(colors.size)],
                velocityX = (cos(angle) * speed).toFloat(),
                velocityY = (sin(angle) * speed).toFloat() - if (isIncome) 200f else 50f,
                isStar = Random.nextBoolean()
            )
        }
    }

    LaunchedEffect(data.timestamp) {
        visible = true
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
        )
        delay(700)
        visible = false
        delay(300)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Particle Canvas Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val curProgress = progress.value
                val particleAlpha = (1f - (curProgress * 0.9f)).coerceIn(0f, 1f)

                particles.forEach { p ->
                    val currentX = centerOffset.x + p.velocityX * curProgress
                    val currentY = centerOffset.y + p.velocityY * curProgress + (200f * curProgress * curProgress) // gravity
                    if (p.isStar) {
                        drawCircle(
                            color = p.color.copy(alpha = particleAlpha),
                            radius = p.radius * (1f - curProgress * 0.5f),
                            center = Offset(currentX, currentY)
                        )
                    } else {
                        drawCircle(
                            color = p.color.copy(alpha = particleAlpha),
                            radius = p.radius * (1f - curProgress * 0.3f),
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }

            // Central Animated Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Badge Icon with Glowing Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.08f))
                                )
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = when {
                                    isIncome -> Icons.Default.ArrowDownward
                                    isExpense -> Icons.Default.ArrowUpward
                                    else -> Icons.Default.SwapHoriz
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Type Title
                    Text(
                        text = when {
                            isIncome -> "Income Received! ✨"
                            isExpense -> "Expense Recorded! 💳"
                            else -> "Transfer Completed! ⇄"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )

                    // Large Formatted Amount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${if (isIncome) "+" else if (isExpense) "-" else ""}${CurrencyUtils.formatCurrency(data.amount, data.currencyCode)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                            fontWeight = FontWeight.Black,
                            color = primaryColor
                        )
                    }

                    // Optional Note or Context Label
                    if (!data.note.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = data.note,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 2
                            )
                        }
                    }

                    // Micro Helper Label
                    Text(
                        text = "Account balances synchronized instantly",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
