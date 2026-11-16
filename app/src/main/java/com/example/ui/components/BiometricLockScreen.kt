package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricHardwareStatus

@Composable
fun BiometricLockScreen(
    savedPin: String,
    onUnlocked: () -> Unit,
    onSetInitialPin: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hardwareStatus = remember(context) { BiometricAuthHelper.getHardwareStatus(context) }
    var enteredPin by remember { mutableStateOf("") }
    var statusMessage by remember {
        mutableStateOf(
            if (hardwareStatus == BiometricHardwareStatus.READY) {
                "Touch the biometric sensor or enter your 4-digit PIN"
            } else if (savedPin.isNotBlank()) {
                "Enter your 4-digit Security PIN to unlock"
            } else {
                "Create a 4-digit Security PIN or tap Biometric Sensor to unlock"
            }
        )
    }
    var isError by remember { mutableStateOf(false) }
    // Wrong-PIN limit, kept across restarts so closing the app does not reset it.
    val lockPrefs = remember(context) { context.getSharedPreferences("ff_pin_guard", android.content.Context.MODE_PRIVATE) }
    var lockedUntil by remember { mutableStateOf(lockPrefs.getLong("locked_until", 0L)) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
        nowMs = System.currentTimeMillis()
    }
    val coolingDown = nowMs < lockedUntil

    fun triggerBiometricPrompt() {
        isError = false
        if (hardwareStatus == BiometricHardwareStatus.READY) {
            BiometricAuthHelper.authenticate(
                context = context,
                title = "Unlock My Finance Vault",
                subtitle = "Authenticate with Fingerprint, Face, or Device Passcode",
                onSuccess = { onUnlocked() },
                onFallbackToPin = { msg ->
                    statusMessage = msg
                },
                onError = { err ->
                    isError = true
                    statusMessage = err
                }
            )
        } else {
            // If running on an emulator or device without enrolled biometrics and no custom PIN set yet
            if (savedPin.isBlank()) {
                onUnlocked()
            } else {
                statusMessage = "Biometrics not enrolled on device — please enter your 4-digit PIN below."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hardwareStatus == BiometricHardwareStatus.READY) {
            triggerBiometricPrompt()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "bio_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = size.minDimension * 0.7f
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = size.minDimension * 0.7f
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("biometric_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Vault Badge & Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Biometric vault protected",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Biometric Orb
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .clickable { triggerBiometricPrompt() }
                        .testTag("btn_biometric_sensor_orb")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "My Finance Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4-Digit PIN Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(if (isFilled) 16.dp else 14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isError) MaterialTheme.colorScheme.error
                                    else if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tactile Numeric Keypad for 4-Digit PIN
            val onDigitPress: (String) -> Unit = { digit ->
                if (coolingDown) {
                    isError = true
                    statusMessage = "Too many wrong PINs. Try again in ${((lockedUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(1)} s or use biometrics."
                } else if (enteredPin.length < 4) {
                    isError = false
                    val nextPin = enteredPin + digit
                    enteredPin = nextPin
                    if (nextPin.length == 4) {
                        if (savedPin.isBlank()) {
                            onSetInitialPin?.invoke(nextPin)
                            onUnlocked()
                        } else if (nextPin == savedPin) {
                            lockPrefs.edit().putInt("fails", 0).putLong("locked_until", 0L).apply()
                            onUnlocked()
                        } else {
                            val fails = lockPrefs.getInt("fails", 0) + 1
                            val wait = if (fails >= 5) (30_000L shl (fails - 5).coerceAtMost(5)).coerceAtMost(15 * 60_000L) else 0L
                            lockedUntil = if (wait > 0) System.currentTimeMillis() + wait else 0L
                            lockPrefs.edit().putInt("fails", fails).putLong("locked_until", lockedUntil).apply()
                            isError = true
                            statusMessage = if (wait > 0) "Too many wrong PINs. Wait ${wait / 1000} s or use biometrics." else "Incorrect PIN. ${5 - fails} tries left before a short wait."
                            enteredPin = ""
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { rowDigits ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowDigits.forEach { digit ->
                            PinPadKey(
                                label = digit,
                                onClick = { onDigitPress(digit) }
                            )
                        }
                    }
                }

                // Bottom row: Biometric button, 0, Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { triggerBiometricPrompt() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("btn_keypad_biometric")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Scan Biometrics",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    PinPadKey(
                        label = "0",
                        onClick = { onDigitPress("0") }
                    )

                    Surface(
                        onClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                isError = false
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("btn_keypad_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Biometric Action Button
            Button(
                onClick = { triggerBiometricPrompt() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_unlock_with_biometrics")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hardwareStatus == BiometricHardwareStatus.READY) {
                        "Unlock with Biometrics"
                    } else {
                        "Verify Biometric / Device Lock"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun PinPadKey(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .size(68.dp)
            .testTag("pin_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
