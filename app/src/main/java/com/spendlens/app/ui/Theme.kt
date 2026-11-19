package com.spendlens.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SpendLensTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF7FD1AE), secondary = Color(0xFF9FD8C0))
        else -> lightColorScheme(primary = Color(0xFF00875A), secondary = Color(0xFF2E7D5B))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun incomeColor(): Color = if (isSystemInDarkTheme()) Color(0xFF81C995) else Color(0xFF1E8E3E)

fun money(v: Double, decimals: Boolean = true): String {
    val f = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    f.maximumFractionDigits = if (decimals) 2 else 0
    f.minimumFractionDigits = if (decimals) 2 else 0
    return f.format(v)
}
