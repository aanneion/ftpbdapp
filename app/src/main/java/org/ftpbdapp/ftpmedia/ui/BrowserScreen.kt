package org.ftpbdapp.ftpmedia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ftpbdapp.ftpmedia.data.Source
import org.ftpbdapp.ftpmedia.ftp.FtpBrowser
import org.ftpbdapp.ftpmedia.ftp.FtpEntry
import org.ftpbdapp.ftpmedia.ftp.MediaTypes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SortMode(val label: String) {
    Name("Name"), Size("Size"), Date("Date modified")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    source: Source,
    onBack: () -> Unit,
    onPlay: (FtpEntry) -> Unit
) {
    val root = source.rootPath.ifBlank { "/" }
    var path by remember { mutableStateOf(root) }
    var entries by remember { mutableStateOf<List<FtpEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortMode.Name) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    var unsupported by remember { mutableStateOf<FtpEntry?>(null) }

    LaunchedEffect(source.id, path, refreshTick) {
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) { runCatching { FtpBrowser.list(source, path) } }
        result.onSuccess { entries = it }
        result.onFailure { e -> error = e.message ?: "Failed to load directory" }
        loading = false
    }

    val visible = remember(entries, search, sort) {
        val filtered = entries.filter { search.isBlank() || it.name.contains(search.trim(), ignoreCase = true) }
        val comparator: Comparator<FtpEntry> = when (sort) {
            SortMode.Name -> compareBy { it.name.lowercase(Locale.getDefault()) }
            SortMode.Size -> compareByDescending { it.size }
            SortMode.Date -> compareByDescending { it.modifiedMillis }
        }
        val folders = filtered.filter { it.isDirectory }.sortedWith(comparator)
        val files = filtered.filterNot { it.isDirectory }.sortedWith(comparator)
        folders + files
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                source.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                path,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (path != root) {
                            IconButton(onClick = { path = parentPath(path, root) }) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up one level")
                            }
                        }
                        IconButton(onClick = { refreshTick += 1 }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        Box {
                            TextButton(onClick = { sortMenuOpen = true }) { Text(sort.label) }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        trailingIcon = {
                                            if (mode == sort) Icon(Icons.Default.Check, contentDescription = null)
                                        },
                                        onClick = { sort = mode; sortMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Filter this folder…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotBlank()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading $path…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                error != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Couldn't load this folder", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { refreshTick += 1 }) { Text("Retry") }
                    }
                }

                visible.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (search.isBlank()) "This folder is empty" else "Nothing matches “$search”",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(visible, key = { it.path }) { entry ->
                            EntryRow(entry) {
                                when {
                                    entry.isDirectory -> path = joinPath(path, entry.name)
                                    MediaTypes.isMedia(entry.name) -> onPlay(entry)
                                    else -> unsupported = entry
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    unsupported?.let { target ->
        AlertDialog(
            onDismissRequest = { unsupported = null },
            title = { Text("Not a media file") },
            text = { Text("“${target.name}” isn't a video or audio file this app can play.") },
            confirmButton = {
                FilledTonalButton(onClick = { unsupported = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun EntryRow(entry: FtpEntry, onClick: () -> Unit) {
    val media = MediaTypes.isMedia(entry.name)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when {
                entry.isDirectory -> Icons.Default.Folder
                MediaTypes.isVideo(entry.name) -> Icons.Default.Movie
                MediaTypes.isAudio(entry.name) -> Icons.Default.MusicNote
                else -> Icons.Default.InsertDriveFile
            }
            val tint = when {
                entry.isDirectory -> MaterialTheme.colorScheme.secondary
                media -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                val meta = buildString {
                    if (!entry.isDirectory && entry.size >= 0) append(formatBytes(entry.size))
                    if (entry.modifiedMillis > 0) {
                        if (isNotEmpty()) append("  ·  ")
                        append(formatDate(entry.modifiedMillis))
                    }
                }
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (media) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun joinPath(current: String, name: String): String =
    if (current.endsWith("/")) current + name else "$current/$name"

private fun parentPath(current: String, root: String): String {
    val trimmed = current.trimEnd('/')
    val index = trimmed.lastIndexOf('/')
    val parent = if (index <= 0) "/" else trimmed.substring(0, index)
    return if (parent.length < root.trimEnd('/').length && root != "/") root else parent
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private fun formatDate(millis: Long): String = dateFormat.format(Date(millis))
