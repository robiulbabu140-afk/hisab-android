package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabPurple

@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("◈", style = MaterialTheme.typography.titleLarge, color = HisabPurple)
        Text("Hisab", style = MaterialTheme.typography.titleLarge, color = HisabPurple)
        Text("Your Money. Your Control.", color = HisabMuted, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onGetStarted, modifier = Modifier.padding(top = 40.dp)) {
            Text("Get Started →")
        }
    }
}
