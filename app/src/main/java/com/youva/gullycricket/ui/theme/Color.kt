package com.youva.gullycricket.ui.theme

import androidx.compose.ui.graphics.Color

// Default template colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Gully Cricket Dark Theme Colors
val CricketDarkBg = Color(0xFF1C1C1E)
val CricketDarkSurface = Color(0xFF2C2C2E)
val CricketDarkTextPrimary = Color(0xFFFFFFFF)
val CricketDarkTextSecondary = Color(0xFFAEAEB2)
val CricketDarkTextAccent = Color(0xFFFFD60A) // Gold
val CricketDarkButtonBg = Color(0xFF3A3A3C)
val CricketDarkWicket = Color(0xFFFF453A)
val CricketDarkBoundary = Color(0xFF30D158)
val CricketDarkTeamA = Color(0xFF0A84FF)
val CricketDarkTeamB = Color(0xFFFF9F0A)
val CricketDarkUmpire = Color(0xFF5E5CE6)

// Gully Cricket Light Theme Colors
val CricketLightBg = Color(0xFFF2F2F7)
val CricketLightSurface = Color(0xFFFFFFFF)
val CricketLightTextPrimary = Color(0xFF000000)
val CricketLightTextSecondary = Color(0xFF6C6C70)
val CricketLightTextAccent = Color(0xFFFF9500) // Orange
val CricketLightButtonBg = Color(0xFFE5E5EA)
val CricketLightWicket = Color(0xFFFF3B30)
val CricketLightBoundary = Color(0xFF34C759)
val CricketLightTeamA = Color(0xFF007AFF)
val CricketLightTeamB = Color(0xFFFF9500)
val CricketLightUmpire = Color(0xFF5856D6)

data class CricketColorScheme(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textAccent: Color,
    val buttonBg: Color,
    val wicket: Color,
    val boundary: Color,
    val teamA: Color,
    val teamB: Color,
    val umpire: Color,
    val grid: Color
)

val CricketDarkColors = CricketColorScheme(
    background = CricketDarkBg,
    surface = CricketDarkSurface,
    textPrimary = CricketDarkTextPrimary,
    textSecondary = CricketDarkTextSecondary,
    textAccent = CricketDarkTextAccent,
    buttonBg = CricketDarkButtonBg,
    wicket = CricketDarkWicket,
    boundary = CricketDarkBoundary,
    teamA = CricketDarkTeamA,
    teamB = CricketDarkTeamB,
    umpire = CricketDarkUmpire,
    grid = Color(0x14FFFFFF) // 8% white
)

val CricketLightColors = CricketColorScheme(
    background = CricketLightBg,
    surface = CricketLightSurface,
    textPrimary = CricketLightTextPrimary,
    textSecondary = CricketLightTextSecondary,
    textAccent = CricketLightTextAccent,
    buttonBg = CricketLightButtonBg,
    wicket = CricketLightWicket,
    boundary = CricketLightBoundary,
    teamA = CricketLightTeamA,
    teamB = CricketLightTeamB,
    umpire = CricketLightUmpire,
    grid = Color(0x0F000000) // 6% black
)
