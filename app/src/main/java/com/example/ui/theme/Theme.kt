package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HDColorScheme = lightColorScheme(
    primary = HDPrimary,
    onPrimary = HDOnPrimary,
    primaryContainer = HDPrimaryContainer,
    onPrimaryContainer = HDOnPrimaryContainer,
    secondary = HDSecondary,
    secondaryContainer = HDSecondaryContainer,
    onSecondaryContainer = HDOnSecondaryContainer,
    background = HDBackground,
    onBackground = HDOnSurface,
    surface = HDSurface,
    onSurface = HDOnSurface,
    surfaceVariant = HDSurfaceVariant,
    onSurfaceVariant = HDOnSurfaceVariant,
    outline = HDOutline,
    error = HDError,
    onError = HDOnError,
    errorContainer = HDErrorContainer,
    onErrorContainer = HDOnErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force consistent light theme matching the custom specification style
    dynamicColor: Boolean = false, // Deactivate Android dynamic colors so our High Density theme is preserved
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HDColorScheme,
        typography = Typography,
        content = content
    )
}
