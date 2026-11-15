package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.sync.ServerSyncManager
import com.example.ui.components.ds.IconBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class ServerApp(val versionCode: Int, val versionName: String, val sizeMb: Double, val url: String)

/**
 * "Check for updates": asks the sync server (same address as Server Sync) which APK it hosts and
 * offers to download it when it's newer than this build. Installing over the app keeps all data.
 */
@Composable
fun AppUpdateRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sync by ServerSyncManager.get(context.applicationContext).state.collectAsState()
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var offer by remember { mutableStateOf<ServerApp?>(null) }

    Surface(
        onClick = {
            if (checking) return@Surface
            checking = true
            message = null
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { fetchServerApp(sync.serverUrl) } }
                checking = false
                result.onSuccess { app ->
                    when {
                        app == null -> message = "No app download on ${sync.serverUrl.removePrefix("https://")} yet"
                        app.versionCode > BuildConfig.VERSION_CODE -> offer = app
                        else -> message = "You're on the latest version (${BuildConfig.VERSION_NAME})"
                    }
                }.onFailure { message = "Couldn't reach the server. Check your connection." }
            }
        },
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(Icons.Rounded.SystemUpdate, size = 38.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Check for updates", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    message ?: "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (checking) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }

    offer?.let { app ->
        AlertDialog(
            onDismissRequest = { offer = null },
            title = { Text("Update available") },
            text = {
                Text("Version ${app.versionName} (${"%.1f".format(app.sizeMb)} MB) is ready. Download it and open the file to install over this app; your data stays.")
            },
            confirmButton = {
                TextButton(onClick = {
                    offer = null
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url))) } catch (_: Exception) { message = "No browser available to download" }
                }) { Text("Download") }
            },
            dismissButton = { TextButton(onClick = { offer = null }) { Text("Later") } }
        )
    }
}

private fun fetchServerApp(serverUrl: String): ServerApp? {
    val base = serverUrl.trimEnd('/')
    val conn = (URL("$base/api/app-version").openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
    }
    try {
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val app = JSONObject(body).optJSONObject("app") ?: return null
        return ServerApp(
            versionCode = app.optInt("versionCode", 0),
            versionName = app.optString("versionName", "?"),
            sizeMb = app.optLong("size", 0) / 1_048_576.0,
            url = base + app.optString("url", "/download/my-finance.apk")
        )
    } finally {
        conn.disconnect()
    }
}
