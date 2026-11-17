package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.components.UpdateDialog
import com.example.ui.components.ds.SectionCard
import com.example.ui.components.ds.StatusPill
import com.example.ui.components.openUrl
import com.example.ui.components.releaseNotesText
import com.example.util.UpdateChecker
import java.text.DateFormat
import java.util.Date

/** Release notes: the real release list from GitHub, with the installed version marked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkReleaseScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var releases by remember { mutableStateOf<List<UpdateChecker.Release>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var offer by remember { mutableStateOf<UpdateChecker.Update?>(null) }

    LaunchedEffect(reload) {
        error = null
        releases = null
        runCatching { UpdateChecker.githubReleases() }
            .onSuccess { releases = it }
            .onFailure { error = "Couldn't load releases from GitHub. Check your connection." }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Release notes", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Installed: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { reload++ }) { Icon(Icons.Rounded.Refresh, contentDescription = "Reload") }
                    IconButton(onClick = { openUrl(context, UpdateChecker.RELEASES_PAGE) }) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = "Open on GitHub")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val list = releases
        when {
            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { reload++ }) { Text("Try again") }
            }
            list == null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (list.isEmpty()) item { Text("No releases published yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { it.versionName }) { r ->
                    val cmp = UpdateChecker.compare(r.versionName, BuildConfig.VERSION_NAME)
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            when {
                                cmp == 0 -> StatusPill("Installed")
                                cmp > 0 && r == list.first() -> StatusPill("New", color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        if (r.publishedAt > 0) {
                            Text(
                                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.publishedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val notes = releaseNotesText(r.notes)
                        if (notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (cmp > 0 && r.apkUrl != null) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    offer = UpdateChecker.Update(r.versionName, r.sizeBytes / 1_048_576.0, r.apkUrl, "GitHub", r.notes)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Update to ${r.versionName}")
                            }
                        }
                    }
                }
            }
        }
    }

    offer?.let { u -> UpdateDialog(u, onDismiss = { offer = null }, onLater = { offer = null }) }
}
