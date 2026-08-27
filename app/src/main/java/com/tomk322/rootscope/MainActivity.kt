package com.tomk322.rootscope

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.tomk322.rootscope.model.Report
import com.tomk322.rootscope.scan.Scanner
import com.tomk322.rootscope.ui.RootscopeScreen
import com.tomk322.rootscope.ui.RootscopeTheme
import com.tomk322.rootscope.ui.ScanState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var scanJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }

            LaunchedEffect(Unit) {
                if (state is ScanState.Idle) {
                    state = ScanState.Running("Probing without root")
                    state = ScanState.Done(Scanner.run(this@MainActivity, requestRoot = false))
                }
            }

            RootscopeTheme {
                RootscopeScreen(
                    state = state,
                    onRescan = { requestRoot ->
                        // Cancel any in-flight scan first: su can block for as long as the
                        // superuser dialog stays on screen, and two overlapping scans would
                        // race to publish their reports.
                        scanJob?.cancel()
                        scanJob = lifecycleScope.launch {
                            state = ScanState.Running(
                                if (requestRoot) "Requesting root - approve the prompt" else "Rescanning",
                            )
                            state = ScanState.Done(
                                Scanner.run(this@MainActivity, requestRoot = requestRoot),
                            )
                        }
                    },
                    onShare = ::shareReport,
                )
            }
        }
    }

    private fun shareReport(report: Report) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rootscope report")
            putExtra(Intent.EXTRA_TEXT, Scanner.toPlainText(report))
        }
        startActivity(Intent.createChooser(intent, "Export report"))
    }
}
