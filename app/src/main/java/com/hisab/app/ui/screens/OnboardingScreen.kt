package com.hisab.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hisab.app.sms.SmsImporter
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.theme.HisabMuted
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) {
            isImporting = true
            scope.launch {
                val count = SmsImporter.importHistorical(context, container.smsRepository)
                isImporting = false
                Toast.makeText(context, "$count টি পুরনো SMS পাওয়া গেছে, Review-এ দেখুন", Toast.LENGTH_LONG).show()
                onDone()
            }
        } else {
            Toast.makeText(
                context,
                "SMS permission ছাড়া transaction auto-detect হবে না — Settings থেকে পরে দিতে পারবেন",
                Toast.LENGTH_LONG
            ).show()
            onDone()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱", style = MaterialTheme.typography.titleLarge)
        Text(
            "SMS Access দরকার",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "bKash/Nagad/Bank SMS থেকে transaction detect করতে Hisab-কে SMS পড়ার অনুমতি দিন। " +
                "কোনো SMS আপনার Review ছাড়া Income/Expense-এ যোগ হবে না।",
            color = HisabMuted,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (isImporting) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            Text("পুরনো SMS Import হচ্ছে...", color = HisabMuted, modifier = Modifier.padding(top = 8.dp))
        } else {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    )
                },
                modifier = Modifier.padding(top = 24.dp)
            ) { Text("Allow SMS Access") }
            TextButton(onClick = onDone, modifier = Modifier.padding(top = 4.dp)) {
                Text("এখন না, পরে করব")
            }
        }
    }
}
