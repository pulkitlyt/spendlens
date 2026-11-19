package com.spendlens.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spendlens.app.data.TransactionRepository
import com.spendlens.app.model.Category
import com.spendlens.app.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(reloadKey: Int) {
    val context = LocalContext.current
    val repo = remember { TransactionRepository(context.applicationContext) }

    var all by remember { mutableStateOf<List<Transaction>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var dataVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey, dataVersion) {
        try {
            all = withContext(Dispatchers.IO) { repo.load() }
            error = null
        } catch (e: Exception) {
            error = "Couldn't read SMS: ${e.message}"
        }
    }

    var month by remember { mutableStateOf(YearMonth.now()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf<Category?>(null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    val zone = remember { ZoneId.systemDefault() }
    val monthTxns = remember(all, month) {
        all.orEmpty().filter { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == month }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(
                            onClick = { month = month.plusMonths(1) },
                            enabled = month < YearMonth.now(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { dataVersion++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Overview") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Transactions") },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val loaded = all
            when {
                error != null -> Text(
                    error.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                loaded == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                tab == 0 -> Overview(
                    txns = monthTxns,
                    onCategoryClick = {
                        filter = it
                        tab = 1
                    },
                )
                else -> TransactionList(
                    txns = monthTxns,
                    filter = filter,
                    onFilter = { filter = it },
                    onClick = { editing = it },
                )
            }
        }
    }

    editing?.let { tx ->
        EditCategoryDialog(
            tx = tx,
            onDismiss = { editing = null },
            onSave = { category, applyToMerchant ->
                repo.overrides.setTxn(tx.id, category)
                if (applyToMerchant) repo.overrides.setMerchant(tx.merchant, category)
                editing = null
                dataVersion++
            },
        )
    }
}
