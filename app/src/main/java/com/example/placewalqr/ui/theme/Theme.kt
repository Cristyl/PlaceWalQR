// Modifica il tuo file esistente Theme.kt sostituendo il contenuto con questo:
package com.example.placewalqr.ui.theme

import android.app.Activity
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

// Palette di verdi per PlaceWalQR
private val Green10 = Color(0xFF002114)
private val Green20 = Color(0xFF003919)
private val Green30 = Color(0xFF005227)
private val Green40 = Color(0xFF006D36)
private val Green50 = Color(0xFF008A47)
private val Green60 = Color(0xFF00A85A)
private val Green70 = Color(0xFF00C46E)
private val Green80 = Color(0xFF5CDB95)
private val Green90 = Color(0xFFA8F5BB)
private val Green95 = Color(0xFFD7FADA)

// Schema colori scuro (sostituisce DarkColorScheme)
private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = Green70,
    tertiary = Green60,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE1E3DF),
    onSurfaceVariant = Color(0xFFC1C9BD),
    background = Color(0xFF11130F),
    onBackground = Color(0xFFE1E3DF)
)

// Schema colori chiaro (sostituisce LightColorScheme)
private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = Green60,
    tertiary = Green50,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    surface = Color(0xFFF8FAF5),
    onSurface = Color(0xFF191C18),
    onSurfaceVariant = Color(0xFF42493F),
    background = Color(0xFFFCFEF7),
    onBackground = Color(0xFF191C18)
)

@Composable
fun PlaceWalQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Cambiato a false per usare i nostri colori verdi
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}