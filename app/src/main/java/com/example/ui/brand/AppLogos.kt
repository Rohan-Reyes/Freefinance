package com.example.ui.brand

import android.content.Context
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.util.BankAppLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real bank / e-wallet logos: the launcher icon of the issuer's app installed on this phone
 * (GCash, MariBank, Maya, Shopee…), provided by Android's PackageManager. Nothing is bundled or
 * downloaded; if the app isn't installed we fall back to the drawn [BrandMark].
 */
object AppLogos {
    private const val ICON_PX = 144
    private val cache = LruCache<String, ImageBitmap>(48)
    private val missing = HashSet<String>()

    /** Loads (and caches) the icon for an account/card name or an explicit package. Call off the main thread. */
    fun load(context: Context, name: String?, explicitPackage: String? = null): ImageBitmap? {
        val app = BankAppLauncher.findMatchingBankApp(name.orEmpty(), explicitPackage) ?: return null
        val key = app.packageName
        cache.get(key)?.let { return it }
        synchronized(missing) { if (key in missing) return null }
        val pm = context.packageManager
        for (pkg in app.allPackages) {
            try {
                val bmp = pm.getApplicationIcon(pkg).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                bmp.prepareToDraw()
                cache.put(key, bmp)
                return bmp
            } catch (_: Exception) {
                // not installed under this package name; try the next one
            }
        }
        synchronized(missing) { missing += key }
        return null
    }
}

@Composable
fun rememberAppLogo(name: String?, explicitPackage: String? = null): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val logo by produceState<ImageBitmap?>(initialValue = null, name, explicitPackage) {
        value = withContext(Dispatchers.IO) { AppLogos.load(context, name, explicitPackage) }
    }
    return logo
}

/** The issuer's real app logo when installed, otherwise the drawn brand mark (or [fallback]). */
@Composable
fun InstitutionLogo(
    name: String?,
    modifier: Modifier = Modifier,
    explicitPackage: String? = null,
    size: Dp = 40.dp,
    fallback: @Composable () -> Unit
) {
    val logo = rememberAppLogo(name, explicitPackage)
    if (logo != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.3f))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = logo, contentDescription = null, modifier = Modifier.size(size))
        }
    } else {
        fallback()
    }
}
