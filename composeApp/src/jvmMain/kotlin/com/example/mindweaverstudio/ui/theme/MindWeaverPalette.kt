package com.example.mindweaverstudio.ui.theme

import androidx.compose.ui.graphics.Color

data class MindWeaverPalette(
    val rootBackground: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    val borderSubtle: Color,
    val borderNeutral: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textInvert: Color,
    val accent300: Color,
    val accent400: Color,
    val accent500: Color,
    val accent600: Color,
    val accent700: Color,
)

val DarkPalette = MindWeaverPalette(
    rootBackground = MindWeaverDarkColors.RootBackground,
    surface1 = MindWeaverDarkColors.Surface1,
    surface2 = MindWeaverDarkColors.Surface2,
    surface3 = MindWeaverDarkColors.Surface3,
    surface4 = MindWeaverDarkColors.Surface4,
    borderSubtle = MindWeaverDarkColors.BorderSubtle,
    borderNeutral = MindWeaverDarkColors.BorderNeutral,
    borderStrong = MindWeaverDarkColors.BorderStrong,
    textPrimary = MindWeaverDarkColors.TextPrimary,
    textSecondary = MindWeaverDarkColors.TextSecondary,
    textMuted = MindWeaverDarkColors.TextMuted,
    textDisabled = MindWeaverDarkColors.TextDisabled,
    textInvert = MindWeaverDarkColors.TextInvert,
    accent300 = MindWeaverDarkColors.Accent300,
    accent400 = MindWeaverDarkColors.Accent400,
    accent500 = MindWeaverDarkColors.Accent500,
    accent600 = MindWeaverDarkColors.Accent600,
    accent700 = MindWeaverDarkColors.Accent700,
)