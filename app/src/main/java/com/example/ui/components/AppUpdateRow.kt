package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.components.ds.IconBadge
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch

/**
 * "Check for updates": looks for a newer version on GitHub Releases and on the sync server and offers
 * to download the newest one. Installing over the app keeps all data.
 */
@Composable
fun AppUpdateRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var offer by remember { mutableStateOf<UpdateChecker.Update?>(null) }

    Surface(
        onClick = {
            if (checking) return@Surface
            checking = true
            message = null
            scope.launch {
                when (val r = UpdateChecker.check(context.applicationContext)) {
                    is UpdateChecker.Result.Available -> offer = r.update
                    UpdateChecker.Result.UpToDate -> message = "You're on the latest version (${BuildConfig.VERSION_NAME})"
                    UpdateChecker.Result.Unreachable -> message = "Couldn't check for updates. Check your connection."
                }
                checking = false
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
                    message ?: "Version ${BuildConfig.VERSION_NAME} · checks GitHub and your sync server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (checking) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }

    offer?.let { u ->
        UpdateDialog(u, onDismiss = { offer = null }, onLater = { offer = null })
    }
}

/** Shared "Update available" prompt (Settings and the daily automatic check). */
@Composable
fun UpdateDialog(update: UpdateChecker.Update, onDismiss: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available: ${update.versionName}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "You have ${BuildConfig.VERSION_NAME}. Download ${update.versionName}" +
                        (if (update.sizeMb > 0) " (${"%.1f".format(update.sizeMb)} MB)" else "") +
                        " from ${update.source} and open the file to install over this app. Your data stays."
                )
                val notes = releaseNotesText(update.notes)
                if (notes.isNotBlank()) {
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                openUrl(context, update.downloadUrl)
            }) { Text("Download") }
        },
        dismissButton = { TextButton(onClick = onLater) { Text("Later") } }
    )
}

/** Release notes are Markdown; show them as plain text without the install boilerplate and checksum. */
fun releaseNotesText(md: String): String = md.lines()
    .filterNot { it.startsWith("SHA-256") }
    .joinToString("\n")
    .replace(Regex("(?s)## Install.*?(?=\\n## |$)"), "")
    .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("\\[(.+?)]\\((.+?)\\)"), "$1")
    .replace(Regex("^- ", RegexOption.MULTILINE), "• ")
    .replace("`", "")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { /* no browser */ }
}
