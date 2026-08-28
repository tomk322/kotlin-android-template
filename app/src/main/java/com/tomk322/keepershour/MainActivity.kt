package com.tomk322.keepershour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tomk322.keepershour.data.SaveStore
import com.tomk322.keepershour.engine.GameEngine
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.KeepersHour
import com.tomk322.keepershour.ui.JournalPanel
import com.tomk322.keepershour.ui.KeepersHourTheme
import com.tomk322.keepershour.ui.SceneScreen
import com.tomk322.keepershour.ui.SeaSurface
import com.tomk322.keepershour.ui.TitleScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SaveStore(this)
        val engine = KeepersHour.newEngine()

        setContent {
            KeepersHourTheme {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                ) { padding ->
                    Game(
                        store = store,
                        engine = engine,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Game(
    store: SaveStore,
    engine: GameEngine,
    modifier: Modifier = Modifier,
) {
    // Loaded once; the save file is the source of truth only across process death.
    val loaded = remember { store.load() }
    var state by remember { mutableStateOf(loaded) }
    var onTitle by remember { mutableStateOf(true) }
    var journalOpen by remember { mutableStateOf(false) }

    fun update(next: GameState) {
        state = next
        store.save(next)
    }

    Box(modifier = modifier) {
        val current = state
        if (onTitle || current == null) {
            TitleScreen(
                hasSave = current != null,
                endingsFound = current?.endingsSeen?.size ?: 0,
                onContinue = { onTitle = false },
                onNewGame = {
                    store.clear()
                    update(engine.initialState())
                    onTitle = false
                },
            )
        } else {
            SceneScreen(
                view = engine.view(current),
                onChoice = { update(engine.apply(current, it)) },
                onJournal = { journalOpen = true },
            )
        }

        if (journalOpen && current != null) {
            ModalBottomSheet(
                onDismissRequest = { journalOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SeaSurface,
            ) {
                JournalPanel(current)
            }
        }
    }
}
