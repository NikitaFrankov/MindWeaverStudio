package com.example.mindweaverstudio.ui.theme

import androidx.compose.runtime.*

val LocalColors = staticCompositionLocalOf { DarkPalette }

@Composable
fun MindWeaverTheme(
    colors: MindWeaverPalette = DarkPalette,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalColors provides colors) {
        content()
    }
}