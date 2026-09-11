package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hisab.app.ui.components.EmptyState

/** Charts/reports are explicitly Phase 2 — see the project plan. This keeps the bottom-nav
 * tab wired up without half-building the reporting feature. */
@Composable
fun ReportsStubScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState("📊", "Reports — coming soon", "Charts and category breakdowns are planned for Phase 2, once the core ledger is confirmed working.")
    }
}
