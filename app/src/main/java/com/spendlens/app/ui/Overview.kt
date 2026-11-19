package com.spendlens.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendlens.app.model.Category
import com.spendlens.app.model.Kind
import com.spendlens.app.model.Transaction

data class CatSum(val category: Category, val total: Double, val count: Int)

private fun summarize(txns: List<Transaction>, kind: Kind): List<CatSum> =
    txns.filter { it.category.kind == kind }
        .groupBy { it.category }
        .map { (c, list) -> CatSum(c, list.sumOf { it.amount }, list.size) }
        .sortedByDescending { it.total }

@Composable
fun Overview(txns: List<Transaction>, onCategoryClick: (Category) -> Unit) {
    val expenses = summarize(txns, Kind.EXPENSE)
    val income = summarize(txns, Kind.INCOME)
    val spent = expenses.sumOf { it.total }
    val received = income.sumOf { it.total }
    val excluded = txns.filter { it.category.kind == Kind.EXCLUDED }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { SummaryCard(spent, received, txns.size) }

        if (txns.isEmpty()) {
            item {
                Text(
                    "No bank or card SMS found for this month.\nTry the previous month with ‹",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                )
            }
        }

        if (expenses.isNotEmpty()) {
            item { DonutCard(expenses, spent) }
            item { SectionTitle("Where your money went") }
            items(expenses, key = { "e" + it.category.name }) { CategoryRow(it, spent, onCategoryClick) }
        }

        if (income.isNotEmpty()) {
            item { SectionTitle("Money in") }
            items(income, key = { "i" + it.category.name }) { CategoryRow(it, received, onCategoryClick) }
        }

        if (excluded.isNotEmpty()) {
            item {
                Text(
                    "${excluded.size} card-bill payment / ignored transaction(s) worth " +
                        "${money(excluded.sumOf { it.amount }, false)} are not counted, so card spends aren't double-counted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        item {
            Text(
                "Tip: tap any transaction to change its category. Tick \"always\" and SpendLens learns that merchant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun SummaryCard(spent: Double, received: Double, count: Int) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Spent", style = MaterialTheme.typography.labelLarge)
                Text(
                    money(spent, false),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Received", style = MaterialTheme.typography.labelLarge)
                Text(
                    money(received, false),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = incomeColor(),
                )
            }
        }
        Text(
            "$count transactions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun DonutCard(slices: List<CatSum>, total: Double) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Donut(slices, total, Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${slices.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("categories", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.take(5).forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(s.category.color)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${s.category.label} ${pct(s.total, total)}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (slices.size > 5) {
                    Text(
                        "+${slices.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Donut(slices: List<CatSum>, total: Double, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
        if (total <= 0.0) return@Canvas
        var start = -90f
        val gap = if (slices.size > 1) 1.5f else 0f
        slices.forEach { s ->
            val sweep = (s.total / total * 360.0).toFloat()
            drawArc(
                color = Color(s.category.color),
                startAngle = start,
                sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun CategoryRow(s: CatSum, total: Double, onClick: (Category) -> Unit) {
    val color = Color(s.category.color)
    val fraction = if (total > 0) (s.total / total).toFloat().coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick(s.category) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { Text(s.category.emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.category.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(money(s.total, false), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(color),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${s.count} txn${if (s.count == 1) "" else "s"} · ${pct(s.total, total)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun pct(part: Double, total: Double): String =
    if (total <= 0) "0%" else "${Math.round(part / total * 100)}%"
