package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.RawSms
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.util.Money

@Composable
fun SmsDetailScreen(navController: NavController, id: Long) {
    val container = LocalAppContainer.current
    var raw by remember { mutableStateOf<RawSms?>(null) }
    LaunchedEffect(id) { raw = container.smsRepository.getById(id) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("SMS Details", onBack = { navController.popBackStack() })
        val current = raw ?: return@Column
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            HisabCard {
                Text("Original SMS", color = HisabMuted, style = MaterialTheme.typography.bodySmall)
                Text(current.body, modifier = Modifier.padding(top = 6.dp))
            }
            HisabCard(modifier = Modifier.padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Sender", color = HisabMuted, modifier = Modifier.padding(end = 8.dp)); Text(current.sender) }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Provider", color = HisabMuted, modifier = Modifier.padding(end = 8.dp)); Text(current.provider.name) }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Detected Type", color = HisabMuted, modifier = Modifier.padding(end = 8.dp)); Text(current.detectedType ?: "—") }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Amount", color = HisabMuted, modifier = Modifier.padding(end = 8.dp)); Text(Money.format(current.amountMinor)) }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Reference", color = HisabMuted, modifier = Modifier.padding(end = 8.dp)); Text(current.reference ?: "—") }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Status", color = HisabMuted, modifier = Modifier.padding(end = 8.dp)); Text(current.status.name) }
            }
        }
    }
}
