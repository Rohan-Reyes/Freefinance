package com.example.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily
import java.util.Locale

/**
 * Brand-styled look for Philippine banks / e-wallets (and a few global ones), recognised from an
 * account's or card's name. These are drawn styles in each issuer's brand colours with its name as a
 * wordmark, not the issuers' official logo or card artwork files.
 */
enum class BrandPattern { WAVES, ORBIT, DIAGONAL, DOTS, ARC }

data class FinanceBrand(
    val id: String,
    val name: String,
    /** Wordmark as the issuer writes its name (e.g. "maya" is lowercase). */
    val wordmark: String,
    /** 1–2 letter monogram for small brand marks. */
    val monogram: String,
    /** Card background gradient, top-left to bottom-right. */
    val colors: List<Color>,
    /** Secondary brand colour used for highlights on the card. */
    val accent: Color,
    /** Text / icon colour on top of [colors]. */
    val content: Color = Color.White,
    val pattern: BrandPattern = BrandPattern.WAVES,
    val italic: Boolean = false,
    internal val keywords: List<String>
)

object Brands {
    val all: List<FinanceBrand> = listOf(
        FinanceBrand("gcash", "GCash", "GCash", "G", listOf(Color(0xFF2D7BFF), Color(0xFF0057E7), Color(0xFF003BB0)), Color(0xFF9CC4FF), pattern = BrandPattern.WAVES, keywords = listOf("gcash", "gsave", "gcredit")),
        FinanceBrand("maya", "Maya", "maya", "m", listOf(Color(0xFF151816), Color(0xFF0E1A13), Color(0xFF06331D)), Color(0xFF00D26A), pattern = BrandPattern.ARC, keywords = listOf("maya", "paymaya")),
        FinanceBrand("maribank", "MariBank", "MariBank", "M", listOf(Color(0xFFFF8A1E), Color(0xFFF26A0A), Color(0xFFD9480F)), Color(0xFFFFE0BF), pattern = BrandPattern.ORBIT, keywords = listOf("maribank", "mari bank")),
        FinanceBrand("seabank", "SeaBank", "SeaBank", "S", listOf(Color(0xFFFF6B3D), Color(0xFFE8491D), Color(0xFFB8340F)), Color(0xFFFFD4C2), pattern = BrandPattern.WAVES, keywords = listOf("seabank", "sea bank")),
        FinanceBrand("gotyme", "GoTyme", "GoTyme", "GT", listOf(Color(0xFF16C7C1), Color(0xFF0B8C9A), Color(0xFF0A3946)), Color(0xFFB8FFF6), pattern = BrandPattern.DIAGONAL, keywords = listOf("gotyme", "go tyme")),
        FinanceBrand("bdo", "BDO", "BDO", "BDO", listOf(Color(0xFF1046B8), Color(0xFF0A3592), Color(0xFF06225F)), Color(0xFFFFC72C), pattern = BrandPattern.ARC, keywords = listOf("bdo", "banco de oro")),
        FinanceBrand("bpi", "BPI", "BPI", "B", listOf(Color(0xFFC8161D), Color(0xFF9E1016), Color(0xFF66090D)), Color(0xFFFFD6D7), pattern = BrandPattern.DIAGONAL, keywords = listOf("bpi", "bank of the philippine islands")),
        FinanceBrand("unionbank", "UnionBank", "UnionBank", "U", listOf(Color(0xFFFF8A00), Color(0xFFE56E00), Color(0xFF3B3B3F)), Color(0xFFFFE2BD), pattern = BrandPattern.ORBIT, keywords = listOf("unionbank", "union bank", "ubp")),
        FinanceBrand("metrobank", "Metrobank", "Metrobank", "M", listOf(Color(0xFF2361C7), Color(0xFF16479A), Color(0xFF0B2A5E)), Color(0xFFCFE0FF), pattern = BrandPattern.WAVES, keywords = listOf("metrobank", "metro bank")),
        FinanceBrand("securitybank", "Security Bank", "Security Bank", "SB", listOf(Color(0xFF14AEDF), Color(0xFF0A7BC2), Color(0xFF07457A)), Color(0xFFD3F3FF), pattern = BrandPattern.DIAGONAL, keywords = listOf("security bank", "securitybank")),
        FinanceBrand("rcbc", "RCBC", "RCBC", "R", listOf(Color(0xFF1B4DA3), Color(0xFF123677), Color(0xFF0A1F47)), Color(0xFFFF4A52), pattern = BrandPattern.ARC, keywords = listOf("rcbc", "diskartech")),
        FinanceBrand("landbank", "LANDBANK", "LANDBANK", "L", listOf(Color(0xFF10A04F), Color(0xFF00783A), Color(0xFF004C24)), Color(0xFFFFD84D), pattern = BrandPattern.WAVES, keywords = listOf("landbank", "land bank")),
        FinanceBrand("pnb", "PNB", "PNB", "P", listOf(Color(0xFF26357F), Color(0xFF1A2560), Color(0xFF10173D)), Color(0xFFFF4D5A), pattern = BrandPattern.ORBIT, keywords = listOf("pnb", "philippine national bank")),
        FinanceBrand("chinabank", "China Bank", "China Bank", "C", listOf(Color(0xFFD0122F), Color(0xFF9A0C22), Color(0xFF5E0715)), Color(0xFFF6D36B), pattern = BrandPattern.ARC, keywords = listOf("china bank", "chinabank")),
        FinanceBrand("eastwest", "EastWest", "EastWest", "E", listOf(Color(0xFF6A35A0), Color(0xFF4B2378), Color(0xFF2C1349)), Color(0xFFFFB42E), pattern = BrandPattern.DIAGONAL, keywords = listOf("eastwest", "east west", "komo")),
        FinanceBrand("tonik", "Tonik", "tonik", "t", listOf(Color(0xFF8A3BFF), Color(0xFF5B1FD6), Color(0xFF2E0E85)), Color(0xFFFF5DB1), pattern = BrandPattern.DOTS, keywords = listOf("tonik")),
        FinanceBrand("cimb", "CIMB", "CIMB", "C", listOf(Color(0xFFEE2A30), Color(0xFFB8161C), Color(0xFF6E0B0F)), Color(0xFFFFD0D1), pattern = BrandPattern.DIAGONAL, keywords = listOf("cimb")),
        FinanceBrand("ownbank", "OwnBank", "OwnBank", "O", listOf(Color(0xFF2B2D42), Color(0xFF1B1C2B)), Color(0xFF7CF29C), pattern = BrandPattern.DOTS, keywords = listOf("ownbank", "own bank")),
        FinanceBrand("shopeepay", "ShopeePay", "ShopeePay", "S", listOf(Color(0xFFF5593A), Color(0xFFE23F1F), Color(0xFFB02A12)), Color(0xFFFFD9CF), pattern = BrandPattern.DOTS, keywords = listOf("shopeepay", "shoppepay", "shopee", "spaylater")),
        FinanceBrand("grabpay", "GrabPay", "GrabPay", "G", listOf(Color(0xFF14C35E), Color(0xFF00A04A), Color(0xFF006B31)), Color(0xFFD5FFE4), pattern = BrandPattern.WAVES, keywords = listOf("grabpay", "grab")),
        FinanceBrand("wise", "Wise", "Wise", "W", listOf(Color(0xFFA6EC79), Color(0xFF8AD65C)), Color(0xFF163300), content = Color(0xFF163300), pattern = BrandPattern.ARC, keywords = listOf("wise", "transferwise")),
        FinanceBrand("revolut", "Revolut", "Revolut", "R", listOf(Color(0xFF2B2F36), Color(0xFF191C1F), Color(0xFF0B0C0E)), Color(0xFFDDE3EA), pattern = BrandPattern.ORBIT, keywords = listOf("revolut")),
        FinanceBrand("paypal", "PayPal", "PayPal", "P", listOf(Color(0xFF0B3B91), Color(0xFF062A6B), Color(0xFF031A45)), Color(0xFF29B6F6), pattern = BrandPattern.WAVES, italic = true, keywords = listOf("paypal"))
    )

    /** First brand whose keyword appears in any of the given names (card name, issuing bank, account). */
    fun detect(vararg names: String?): FinanceBrand? {
        for (raw in names) {
            if (raw.isNullOrBlank()) continue
            val lower = raw.lowercase(Locale.ROOT)
            val squashed = lower.filter { it.isLetterOrDigit() }
            val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
            for (brand in all) {
                val hit = brand.keywords.any { kw ->
                    val k = kw.filter { it.isLetterOrDigit() }
                    // Short keywords ("bdo", "bpi", "wise") must be whole words; long ones may be glued ("gcashsave").
                    if (k.length <= 4) k in tokens else squashed.contains(k)
                }
                if (hit) return brand
            }
        }
        return null
    }
}

/** Rounded-square brand mark (gradient + monogram) used as the leading symbol for accounts. */
@Composable
fun BrandMark(brand: FinanceBrand, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(brand.colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = brand.monogram,
            color = if (brand.id == "maya") brand.accent else brand.content,
            style = TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = if (brand.italic) FontStyle.Italic else FontStyle.Normal,
                fontSize = (size.value * when (brand.monogram.length) { 1 -> 0.48f; 2 -> 0.36f; else -> 0.27f }).sp
            )
        )
    }
}

/** The issuer's name set as a wordmark on the card face. */
@Composable
fun BrandWordmark(brand: FinanceBrand, modifier: Modifier = Modifier, fontSize: TextUnit = 22.sp) {
    Text(
        text = brand.wordmark,
        modifier = modifier,
        color = if (brand.id == "maya") brand.accent else brand.content,
        style = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = if (brand.italic) FontStyle.Italic else FontStyle.Normal,
            fontSize = fontSize,
            letterSpacing = if (brand.wordmark.all { it.isUpperCase() || !it.isLetter() }) 1.5.sp else (-0.3).sp
        ),
        maxLines = 1
    )
}

/** Subtle background artwork for a card face, in the brand's accent colour. */
fun DrawScope.drawBrandPattern(pattern: BrandPattern, accent: Color) {
    val w = size.width
    val h = size.height
    when (pattern) {
        BrandPattern.WAVES -> {
            for (i in 0 until 5) {
                val y = h * (0.55f + i * 0.09f)
                drawArc(
                    color = accent.copy(alpha = 0.10f - i * 0.012f),
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(-w * 0.2f, y - h * 0.25f), size = Size(w * 1.4f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        BrandPattern.ORBIT -> {
            for (i in 1..4) {
                drawCircle(accent.copy(alpha = 0.16f - i * 0.03f), radius = h * 0.28f * i, center = Offset(w * 0.92f, h * 0.05f), style = Stroke(width = 1.5.dp.toPx()))
            }
        }
        BrandPattern.DIAGONAL -> {
            val step = 18.dp.toPx()
            var x = w * 0.45f
            while (x < w * 1.6f) {
                drawLine(accent.copy(alpha = 0.08f), Offset(x, 0f), Offset(x - h, h), strokeWidth = 6.dp.toPx())
                x += step * 2
            }
        }
        BrandPattern.DOTS -> {
            val step = 14.dp.toPx()
            var y = step
            while (y < h) {
                var x = w * 0.55f + (y / step % 2) * step / 2
                while (x < w) {
                    drawCircle(accent.copy(alpha = 0.16f * (x / w)), radius = 1.6.dp.toPx(), center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        BrandPattern.ARC -> {
            drawCircle(accent.copy(alpha = 0.14f), radius = h * 0.9f, center = Offset(w * 1.05f, h * 1.1f))
            drawCircle(accent.copy(alpha = 0.10f), radius = h * 0.55f, center = Offset(w * 1.05f, h * 1.1f))
        }
    }
}

/** Card network mark drawn as a symbol (Mastercard circles, Visa wordmark, etc.). */
@Composable
fun NetworkMark(network: String, modifier: Modifier = Modifier, onDark: Boolean = true, height: Dp = 24.dp) {
    val ink = if (onDark) Color.White else Color(0xFF14213D)
    when (network.uppercase(Locale.ROOT)) {
        "MASTERCARD" -> Canvas(modifier = modifier.size(width = height * 1.62f, height = height)) {
            val r = size.height / 2f
            val left = Offset(r, r)
            val right = Offset(size.width - r, r)
            drawCircle(Color(0xFFEB001B), r, left)
            drawCircle(Color(0xFFF79E1B), r, right)
            val lens = Path().apply {
                op(
                    Path().apply { addOval(Rect(left, r)) },
                    Path().apply { addOval(Rect(right, r)) },
                    PathOperation.Intersect
                )
            }
            drawPath(lens, Color(0xFFFF5F00))
        }
        "VISA" -> Text(
            "VISA", modifier = modifier,
            color = ink,
            style = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = (height.value * 0.85f).sp, letterSpacing = 0.5.sp)
        )
        "AMEX" -> Box(modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2E77BC)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text("AMEX", color = Color.White, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = (height.value * 0.5f).sp, letterSpacing = 1.sp))
        }
        "JCB" -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            listOf(Color(0xFF0B4EA2), Color(0xFFE21836), Color(0xFF00A651)).forEachIndexed { i, c ->
                Box(Modifier.size(width = height * 0.5f, height = height).clip(RoundedCornerShape(4.dp)).background(c), contentAlignment = Alignment.Center) {
                    Text("JCB"[i].toString(), color = Color.White, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = (height.value * 0.45f).sp))
                }
            }
        }
        "UNIONPAY" -> Text("UnionPay", modifier = modifier, color = ink, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = (height.value * 0.6f).sp))
        else -> Text(network.uppercase(Locale.ROOT), modifier = modifier, color = ink.copy(alpha = 0.9f), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = (height.value * 0.5f).sp, letterSpacing = 1.sp))
    }
}
