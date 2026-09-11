package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.CategoryKind
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(CategoryKind.EXPENSE) }
    val categories by container.categoryRepository.observeByKind(kind).collectAsState(initial = emptyList())
    var newName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Categories", onBack = { navController.popBackStack() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            listOf(CategoryKind.EXPENSE, CategoryKind.INCOME).forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    label = { Text(k.name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            items(categories, key = { it.id }) { cat: Category ->
                HisabCard(modifier = Modifier.padding(top = 10.dp)) {
                    Text("${cat.icon} ${cat.name}")
                }
            }
            item {
                HisabCard(modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("New category name") })
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                scope.launch {
                                    container.categoryRepository.create(
                                        Category(name = newName, icon = "🏷️", kind = kind, isCustom = true)
                                    )
                                    newName = ""
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Add Category") }
                }
            }
        }
    }
}
