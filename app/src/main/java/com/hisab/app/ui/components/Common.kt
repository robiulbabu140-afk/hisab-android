package com.hisab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.theme.HisabGradientColors
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money

@Composable
fun HisabCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        trailing?.invoke()
    }
}

/** The app's signature moment — a gradient hero card for "Total Available Balance", with up to
 * three compact sub-figures underneath (e.g. This Month Income/Expense/Managed). Mirrors the
 * web dashboard's .dbalance card exactly so the app and the web read as one product. */
@Composable
fun GradientBalanceCard(label: String, amountMinor: Long, subItems: List<Pair<String, Long>> = emptyList()) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(colors = HisabGradientColors, start = Offset(0f, 0f), end = Offset(600f, 600f)),
                RoundedCornerShape(24.dp)
            )
            .padding(22.dp)
    ) {
        Column {
            Text(label.uppercase(), color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
            Text(
                Money.format(amountMinor),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                modifier = Modifier.padding(top = 8.dp, bottom = if (subItems.isEmpty()) 0.dp else 16.dp)
            )
            if (subItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    subItems.forEach { (subLabel, subAmount) ->
                        Column {
                            Text(subLabel, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                            Text(Money.format(subAmount), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** A compact metric card with a small leading icon badge — e.g. This Month's Income/Expense. */
@Composable
fun StatTile(
    label: String,
    valueMinor: Long,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: String? = null,
    modifier: Modifier = Modifier
) {
    HisabCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = HisabMuted)
            if (icon != null) {
                Box(
                    modifier = Modifier.size(26.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(icon, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Text(Money.format(valueMinor), style = MaterialTheme.typography.titleMedium, color = valueColor, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun TransactionRow(
    icon: String,
    title: String,
    subtitle: String,
    amountMinor: Long,
    type: TxnType,
    onClick: (() -> Unit)? = null
) {
    val color = when (type) {
        TxnType.INCOME -> HisabGreen
        TxnType.EXPENSE -> HisabRed
        TxnType.TRANSFER, TxnType.NEUTRAL -> HisabMuted
    }
    val prefix = when (type) {
        TxnType.INCOME -> "+"
        TxnType.EXPENSE -> "-"
        else -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(icon) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HisabMuted)
        }
        Text("$prefix${Money.format(amountMinor)}", color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action != null) {
            Text(
                action,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = onAction?.let { Modifier.clickable(onClick = it) } ?: Modifier
            )
        }
    }
}

@Composable
fun ChipPicker(label: String, options: List<Pair<Long, String>>, selectedId: Long?, onSelect: (Long) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        options.forEach { (id, text) ->
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelect(id) },
                label = { Text(text) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(message, color = HisabMuted, modifier = Modifier.padding(top = 4.dp))
    }
}
