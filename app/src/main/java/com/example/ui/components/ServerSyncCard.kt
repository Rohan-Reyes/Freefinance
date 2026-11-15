package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.sync.ServerSyncManager
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Settings card: sign in to the FreeFinance sync server and keep this phone in sync
 * with the web app and other devices.
 */
@Composable
fun ServerSyncCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember { ServerSyncManager.get(context.applicationContext) }
    val state by manager.state.collectAsState()
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(state.serverUrl) }
    var username by remember { mutableStateOf(state.username) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var askReplace by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Server Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sync with the web app and your other devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.syncing || busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (state.signedIn && state.initialized) {
                // ---------------- Signed in ----------------
                val last = if (state.lastSyncAt > 0) {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(state.lastSyncAt))
                } else "Never"
                Text("Signed in as ${state.username}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(state.serverUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Last sync: $last  ·  Pending changes: ${state.pendingChanges}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Auto sync is on: right after changes and every 45 s while the app is open, about every 15 min in the background. Currency and theme sync too.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.lastError?.let {
                    Text("Last error: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !state.syncing && !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                message = try {
                                    manager.syncNow(); "Synced"
                                } catch (e: Exception) {
                                    e.message ?: "Sync failed"
                                }
                                busy = false
                            }
                        }
                    ) { Text("Sync now") }
                    OutlinedButton(onClick = { confirmSignOut = true }, enabled = !busy) { Text("Sign out") }
                }
            } else {
                // ---------------- Sign-in form ----------------
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Server address") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Create the account first on the web app (it asks for the one-time setup code).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    enabled = !busy && username.isNotBlank() && password.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                val info = manager.signIn(url, username, password)
                                password = ""
                                when {
                                    info.serverRecordCount == 0 -> {
                                        manager.beginByUploadingLocal()
                                        message = "Uploaded this phone's data to the server"
                                    }
                                    !info.localHasData -> {
                                        manager.beginByReplacingLocal()
                                        message = "Downloaded your data from the server"
                                    }
                                    else -> askReplace = true
                                }
                            } catch (e: Exception) {
                                message = e.message ?: "Sign-in failed"
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sign in & sync") }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (askReplace) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Server already has data") },
            text = {
                Text(
                    "Both this phone and the server contain financial records. To keep one consistent ledger, " +
                        "this phone's data will be REPLACED by the server's data.\n\n" +
                        "Tip: export a JSON backup first (Settings → Data) if you want to keep a copy."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askReplace = false
                    scope.launch {
                        busy = true
                        message = try {
                            manager.beginByReplacingLocal(); "Downloaded your data from the server"
                        } catch (e: Exception) {
                            e.message ?: "Sync failed"
                        }
                        busy = false
                    }
                }) { Text("Replace phone data") }
            },
            dismissButton = {
                TextButton(onClick = {
                    askReplace = false
                    scope.launch { manager.signOut() }
                    message = "Cancelled — nothing was changed"
                }) { Text("Cancel") }
            }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out of sync?") },
            text = { Text("Your data stays on this phone, but it will stop syncing with the server.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    scope.launch { manager.signOut() }
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Keep syncing") } }
        )
    }
}
