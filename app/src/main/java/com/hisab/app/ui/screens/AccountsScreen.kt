package com.hisab.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.Account
import com.hisab.app.data.local.AccountType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Accounts", onBack = { navController.popBackStack() })
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            items(accounts, key = { it.id }) { acc ->
                HisabCard(modifier = Modifier.padding(bottom = 10.dp).clickable { navController.navigate(Dest.accountDetail(acc.id)) }) {
                    Text("${acc.icon} ${acc.name}")
                    Text(Money.format(acc.balanceMinor))
                }
            }
            item {
                if (showAdd) {
                    HisabCard(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Account name") })
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    scope.launch {
                                        container.accountRepository.create(Account(name = newName, type = AccountType.OTHER, icon = "💳"))
                                        newName = ""
                                        showAdd = false
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Save Account") }
                    }
                } else {
                    Button(onClick = { showAdd = true }, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                        Text("＋ Add New Account")
                    }
                }
            }
        }
    }
}
