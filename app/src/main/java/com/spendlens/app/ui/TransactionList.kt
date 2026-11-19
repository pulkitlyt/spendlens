package com.spendlens.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendlens.app.model.Category
import com.spendlens.app.model.Kind
import com.spendlens.app.model.Transaction
import com.spendlens.app.model.TxnType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")

private fun Transaction.zoned() = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    txns: List<Transaction>,
    filter: Category?,
    onFilter: (Category?) -> Unit,
    onClick: (Transaction) -> Unit,
) {
    val cats = (txns.map { it.category } + listOfNotNull(filter)).distinct().sortedBy { it.ordinal }
    val shown = if (filter == null) txns else txns.filter { it.category == filter }
    val grouped: Map<LocalDate, List<Transaction>> = shown.groupBy { it.zoned().toLocalDate() }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selected = filter == null, onClick = { onFilter(null) }, label = { Text("All") }) }
            items(cats, key = { it.name }) { c ->
                FilterChip(
                    selected = filter == c,
                    onClick = { onFilter(if (filter == c) null else c) },
                    label = { Text("${c.emoji} ${c.label}") },
                )
            }
        }

        if (shown.isEmpty()) {
            Text(
                "Nothing here for this month.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            )
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            grouped.forEach { (day, list) ->
                item(key = "h$day") { DayHeader(day, list) }
                items(list, key = { it.id }) { TxnRow(it, onClick) }
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, list: List<Transaction>) {
    val spent = list.filter { it.category.kind == Kind.EXPENSE }.sumOf { it.amount }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Text(
            day.format(dayFmt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (spent > 0) {
            Text(
                "−${money(spent, false)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TxnRow(tx: Transaction, onClick: (Transaction) -> Unit) {
    val excluded = tx.category.kind == Kind.EXCLUDED
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick(tx) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(tx.category.color).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { Text(tx.category.emoji, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.merchant,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(tx.category.label, tx.zoned().format(timeFmt), tx.account?.let { "••$it" })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (tx.type == TxnType.DEBIT) "−" else "+") + money(tx.amount),
            fontWeight = FontWeight.SemiBold,
            color = when {
                excluded -> MaterialTheme.colorScheme.onSurfaceVariant
                tx.type == TxnType.CREDIT -> incomeColor()
                else -> MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (excluded) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
fun EditCategoryDialog(
    tx: Transaction,
    onDismiss: () -> Unit,
    onSave: (Category, Boolean) -> Unit,
) {
    var selected by remember { mutableStateOf(tx.category) }
    var always by remember { mutableStateOf(false) }
    val options = Category.entries.filter { it.allowedFor(tx.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx.merchant, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${if (tx.type == TxnType.DEBIT) "Paid" else "Received"} ${money(tx.amount)} · ${tx.zoned().format(fullFmt)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(tx.sender, style = MaterialTheme.typography.labelSmall)
                        Text(tx.body, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                options.forEach { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = c },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == c, onClick = { selected = c })
                        Text("${c.emoji}  ${c.label}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { always = !always },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = always, onCheckedChange = { always = it })
                    Text(
                        "Always use this for \"${tx.merchant}\"",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected, always) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
