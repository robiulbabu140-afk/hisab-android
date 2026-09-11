package com.hisab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money

@Composable
fun HisabCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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

@Composable
fun StatTile(label: String, valueMinor: Long, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, modifier: Modifier = Modifier) {
    HisabCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = HisabMuted)
        Text(Money.format(valueMinor), style = MaterialTheme.typography.titleMedium, color = valueColor)
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
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
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
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
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
