package org.ftpbdapp.ftpmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ftpbdapp.ftpmedia.data.Source
import org.ftpbdapp.ftpmedia.data.SourcesStore
import org.ftpbdapp.ftpmedia.ftp.FtpBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    store: SourcesStore,
    onOpenSource: (Source) -> Unit
) {
    var sources by remember { mutableStateOf(store.load()) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Source?>(null) }
    var pendingDelete by remember { mutableStateOf<Source?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("FTP Media", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add source") }
            )
        }
    ) { padding ->
        if (sources.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding), onAdd = { editing = null; showEditor = true })
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Your servers",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(sources, key = { it.id }) { source ->
                    SourceCard(
                        source = source,
                        onOpen = { onOpenSource(source) },
                        onEdit = { editing = source; showEditor = true },
                        onDelete = { pendingDelete = source }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showEditor) {
        SourceEditorDialog(
            store = store,
            initial = editing,
            onDismiss = { showEditor = false },
            onSaved = {
                sources = store.load()
                showEditor = false
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove source") },
            text = { Text("Remove “${target.name}” from this device? The server itself is not affected.") },
            confirmButton = {
                FilledTonalButton(onClick = {
                    store.delete(target.id)
                    sources = store.load()
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(24.dp)
                    .size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("No sources yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add any FTP or FTPS server you're allowed to use — your own NAS, a home server, or a lawful open directory — and stream its media library from one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) { Text("Add your first source") }
    }
}

@Composable
private fun SourceCard(
    source: Source,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(30.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    source.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${source.host}:${source.port}  ·  ${source.rootPath.ifBlank { "/" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (source.useFtps) "FTPS" else "FTP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (source.username.isBlank()) "anonymous" else source.username,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceEditorDialog(
    store: SourcesStore,
    initial: Source?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf(initial?.port?.toString() ?: "21") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var rootPath by remember { mutableStateOf(initial?.rootPath ?: "/") }
    var useFtps by remember { mutableStateOf(initial?.useFtps ?: false) }
    var showPassword by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val portNumber = port.toIntOrNull()
    val valid = name.isNotBlank() && host.isNotBlank() && portNumber != null && portNumber in 1..65535

    fun candidate() = Source(
        id = initial?.id ?: store.newId(),
        name = name.trim(),
        host = host.trim(),
        port = portNumber ?: 21,
        username = username.trim(),
        password = password,
        rootPath = rootPath.trim().ifBlank { "/" },
        useFtps = useFtps
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add source" else "Edit source") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Display name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host, onValueChange = { host = it },
                            label = { Text("Host") },
                            placeholder = { Text("ftp.example.com") },
                            singleLine = true, modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = port, onValueChange = { port = it.filter(Char::isDigit) },
                            label = { Text("Port") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            label = { Text("Username") },
                            placeholder = { Text("anonymous") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(checked = useFtps, onCheckedChange = { useFtps = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Use FTPS (explicit TLS)")
                        }
                        Text(
                            if (showPassword) "Hide" else "Show",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showPassword = !showPassword }
                                .padding(8.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = rootPath, onValueChange = { rootPath = it },
                        label = { Text("Start path") },
                        placeholder = { Text("/") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    FilledTonalButton(
                        enabled = valid && !testing,
                        onClick = {
                            scope.launch {
                                testing = true
                                testResult = null
                                val result = withContext(Dispatchers.IO) { FtpBrowser.test(candidate()) }
                                testResult = result
                                testing = false
                            }
                        }
                    ) {
                        if (testing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Testing…")
                        } else {
                            Text("Test connection")
                        }
                    }
                }
                testResult?.let { message ->
                    item {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.startsWith("Connected")) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledIconButton(
                onClick = { if (valid) { store.upsert(candidate()); onSaved() } },
                enabled = valid
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        },
        dismissButton = {
            FilledIconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        }
    )
}
