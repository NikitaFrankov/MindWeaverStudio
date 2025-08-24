package com.example.mindweaverstudio.ui.theme

import androidx.compose.ui.graphics.Color

data class MindWeaverPalette(
    // Background and Surfaces
    val rootBackground: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    
    // Borders
    val borderSubtle: Color,
    val borderNeutral: Color,
    val borderStrong: Color,
    
    // Text Colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textInvert: Color,
    
    // Accent Colors
    val accent300: Color,
    val accent400: Color,
    val accent500: Color,
    val accent600: Color,
    val accent700: Color,
    
    // Semantic Colors - Success
    val success: Color,
    val successSurface: Color,
    val successBorder: Color,
    
    // Semantic Colors - Warning
    val warning: Color,
    val warningSurface: Color,
    val warningBorder: Color,
    
    // Semantic Colors - Error
    val error: Color,
    val errorSurface: Color,
    val errorBorder: Color,
    
    // Semantic Colors - Info
    val info: Color,
    val infoSurface: Color,
    val infoBorder: Color,
    
    // Editor Specific Colors
    val selection: Color,
    val caret: Color,
    val matchBrackets: Color,
)

val DarkPalette = MindWeaverPalette(
    // Background and Surfaces
    rootBackground = MindWeaverDarkColors.RootBackground,
    surface1 = MindWeaverDarkColors.Surface1,
    surface2 = MindWeaverDarkColors.Surface2,
    surface3 = MindWeaverDarkColors.Surface3,
    surface4 = MindWeaverDarkColors.Surface4,

    // Borders
    borderSubtle = MindWeaverDarkColors.BorderSubtle,
    borderNeutral = MindWeaverDarkColors.BorderNeutral,
    borderStrong = MindWeaverDarkColors.BorderStrong,

    // Text Colors
    textPrimary = MindWeaverDarkColors.TextPrimary,
    textSecondary = MindWeaverDarkColors.TextSecondary,
    textMuted = MindWeaverDarkColors.TextMuted,
    textDisabled = MindWeaverDarkColors.TextDisabled,
    textInvert = MindWeaverDarkColors.TextInvert,

    // Accent Colors
    accent300 = MindWeaverDarkColors.Accent300,
    accent400 = MindWeaverDarkColors.Accent400,
    accent500 = MindWeaverDarkColors.Accent500,
    accent600 = MindWeaverDarkColors.Accent600,
    accent700 = MindWeaverDarkColors.Accent700,

    // Semantic Colors - Success
    success = MindWeaverDarkColors.Success,
    successSurface = MindWeaverDarkColors.SuccessSurface,
    successBorder = MindWeaverDarkColors.SuccessBorder,

    // Semantic Colors - Warning
    warning = MindWeaverDarkColors.Warning,
    warningSurface = MindWeaverDarkColors.WarningSurface,
    warningBorder = MindWeaverDarkColors.WarningBorder,

    // Semantic Colors - Error
    error = MindWeaverDarkColors.Error,
    errorSurface = MindWeaverDarkColors.ErrorSurface,
    errorBorder = MindWeaverDarkColors.ErrorBorder,

    // Semantic Colors - Info
    info = MindWeaverDarkColors.Info,
    infoSurface = MindWeaverDarkColors.InfoSurface,
    infoBorder = MindWeaverDarkColors.InfoBorder,

    // Editor Specific Colors
    selection = MindWeaverDarkColors.Selection,
    caret = MindWeaverDarkColors.Caret,
    matchBrackets = MindWeaverDarkColors.MatchBrackets,
)