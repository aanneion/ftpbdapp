package org.ftpbdapp.ftpmedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.ftpbdapp.ftpmedia.data.Source
import org.ftpbdapp.ftpmedia.data.SourcesStore
import org.ftpbdapp.ftpmedia.ftp.FtpEntry
import org.ftpbdapp.ftpmedia.ui.BrowserScreen
import org.ftpbdapp.ftpmedia.ui.PlayerScreen
import org.ftpbdapp.ftpmedia.ui.SourcesScreen
import org.ftpbdapp.ftpmedia.ui.theme.AppTheme

private sealed interface Screen {
    data object Sources : Screen
    data class Browser(val source: Source) : Screen
    data class Player(val source: Source, val entry: FtpEntry) : Screen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = SourcesStore(this)

        setContent {
            AppTheme {
                var screen by mutableStateOf<Screen>(Screen.Sources)

                when (val current = screen) {
                    is Screen.Sources -> {
                        Surface(Modifier.fillMaxSize()) {
                            SourcesScreen(
                                store = store,
                                onOpenSource = { screen = Screen.Browser(it) }
                            )
                        }
                    }

                    is Screen.Browser -> {
                        BackHandler { screen = Screen.Sources }
                        Surface(Modifier.fillMaxSize()) {
                            BrowserScreen(
                                source = current.source,
                                onBack = { screen = Screen.Sources },
                                onPlay = { entry -> screen = Screen.Player(current.source, entry) }
                            )
                        }
                    }

                    is Screen.Player -> {
                        PlayerScreen(
                            source = current.source,
                            entry = current.entry,
                            onBack = { screen = Screen.Browser(current.source) }
                        )
                    }
                }
            }
        }
    }
}
