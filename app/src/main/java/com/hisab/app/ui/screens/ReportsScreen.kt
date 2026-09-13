package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.Transaction
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ChipPicker
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.components.SectionHeader
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Reads straight from the already-synced local Room database — no network call — so opening
 * this tab or switching months is instant, with no loading spinner or delay.
 */
@Composable
fun ReportsScreen() {
    val container = LocalAppContainer.current
    val zone = remember { ZoneId.systemDefault() }
    val currentMonth = remember { YearMonth.now(zone) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    var txns by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(selectedMonth) {
        val start = selectedMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = selectedMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        txns = container.transactionRepository.getBetween(start, end)
        categories = container.categoryRepository.getAllOnce()
    }

    val categoriesById = categories.associateBy { it.id }
    val income = txns.filter { it.type == TxnType.INCOME }
    val expense = txns.filter { it.type == TxnType.EXPENSE }
    val incomeTotal = income.sumOf { it.amountMinor }
    val expenseTotal = expense.sumOf { it.amountMinor }
    val net = incomeTotal - expenseTotal

    val monthOptions = remember(currentMonth) {
        (0..5).map { currentMonth.minusMonths(it.toLong()) }
            .map { it.toEpochMonth() to it.readableLabel() }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Reports")
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            item {
                ChipPicker("Month", monthOptions, selectedMonth.toEpochMonth()) { epoch ->
                    selectedMonth = YearMonth.of((epoch / 12).toInt(), (epoch % 12 + 1).toInt())
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    HisabCard(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text("Income", style = MaterialTheme.typography.bodySmall, color = HisabMuted)
                        Text(Money.format(incomeTotal), style = MaterialTheme.typography.titleMedium, color = HisabGreen)
                    }
                    HisabCard(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                        Text("Expense", style = MaterialTheme.typography.bodySmall, color = HisabMuted)
                        Text(Money.format(expenseTotal), style = MaterialTheme.typography.titleMedium, color = HisabRed)
                    }
                }
            }
            item {
                HisabCard(modifier = Modifier.padding(top = 10.dp)) {
                    Text("Net", style = MaterialTheme.typography.bodySmall, color = HisabMuted)
                    Text(
                        (if (net >= 0) "+" else "") + Money.format(net),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (net >= 0) HisabGreen else HisabRed
                    )
                }
            }
            item { SectionHeader("Expense by Category") }
            if (expense.isEmpty()) {
                item { EmptyState("📊", "কোনো expense নেই", "এই মাসে কোনো expense transaction নেই।") }
            } else {
                items(categoryBreakdown(expense, categoriesById), key = { "e_${it.name}" }) { row ->
                    CategoryBar(row, expenseTotal, HisabRed)
                }
            }
            item { SectionHeader("Income by Category") }
            if (income.isEmpty()) {
                item { EmptyState("📊", "কোনো income নেই", "এই মাসে কোনো income transaction নেই।") }
            } else {
                items(categoryBreakdown(income, categoriesById), key = { "i_${it.name}" }) { row ->
                    CategoryBar(row, incomeTotal, HisabGreen)
                }
            }
        }
    }
}

private data class CategoryTotal(val icon: String, val name: String, val amountMinor: Long)

private fun categoryBreakdown(txns: List<Transaction>, categoriesById: Map<Long, Category>): List<CategoryTotal> =
    txns.groupBy { it.categoryId }
        .map { (categoryId, group) ->
            val cat = categoryId?.let { categoriesById[it] }
            CategoryTotal(cat?.icon ?: "❓", cat?.name ?: "Other", group.sumOf { it.amountMinor })
        }
        .sortedByDescending { it.amountMinor }

@Composable
private fun CategoryBar(row: CategoryTotal, total: Long, color: Color) {
    HisabCard(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("${row.icon} ${row.name}", modifier = Modifier.weight(1f))
            Text(Money.format(row.amountMinor), color = color)
        }
        LinearProgressIndicator(
            progress = { if (total <= 0) 0f else (row.amountMinor.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = color
        )
    }
}

private fun YearMonth.toEpochMonth(): Long = year * 12L + (monthValue - 1)
private fun YearMonth.readableLabel(): String = "${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year"
