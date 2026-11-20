package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun cobaltLight() = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CobaltOnPrimary,
    primaryContainer = CobaltPrimaryContainer,
    onPrimaryContainer = CobaltOnPrimaryContainer,
    secondary = CobaltSecondary,
    onSecondary = CobaltOnPrimary,
    secondaryContainer = CobaltSecondaryContainer,
    onSecondaryContainer = CobaltOnSecondaryContainer,
    tertiary = CobaltTertiary,
    onTertiary = CobaltOnPrimary,
    tertiaryContainer = CobaltTertiaryContainer,
    onTertiaryContainer = CobaltOnTertiaryContainer,
    background = CobaltCanvas,
    onBackground = CobaltInk,
    surface = CobaltSurface,
    onSurface = CobaltInk,
    surfaceVariant = CobaltSurfaceVariant,
    onSurfaceVariant = CobaltMutedInk,
    surfaceContainerLowest = CobaltSurface,
    surfaceContainerLow = CobaltCanvas,
    surfaceContainer = CobaltSurfaceVariant,
    surfaceContainerHigh = CobaltSurfaceHigh,
    outline = CobaltOutline,
    outlineVariant = CobaltOutlineVariant,
    error = CobaltTertiary,
    onError = CobaltOnPrimary,
    errorContainer = CobaltTertiaryContainer,
    onErrorContainer = CobaltOnTertiaryContainer
)

private fun cobaltDark() = darkColorScheme(
    primary = DarkCobaltPrimary,
    onPrimary = DarkCobaltOnPrimary,
    primaryContainer = DarkCobaltPrimaryContainer,
    onPrimaryContainer = DarkCobaltOnPrimaryContainer,
    secondary = DarkCobaltSecondary,
    onSecondary = DarkCobaltOnPrimary,
    secondaryContainer = DarkCobaltSecondaryContainer,
    onSecondaryContainer = DarkCobaltOnSecondaryContainer,
    tertiary = DarkCobaltTertiary,
    onTertiary = DarkCobaltOnPrimary,
    tertiaryContainer = DarkCobaltTertiaryContainer,
    onTertiaryContainer = DarkCobaltOnTertiaryContainer,
    background = DarkCobaltCanvas,
    onBackground = DarkCobaltInk,
    surface = DarkCobaltSurface,
    onSurface = DarkCobaltInk,
    surfaceVariant = DarkCobaltSurfaceVariant,
    onSurfaceVariant = DarkCobaltMutedInk,
    surfaceContainerLowest = DarkCobaltCanvas,
    surfaceContainerLow = DarkCobaltSurface,
    surfaceContainer = DarkCobaltSurfaceVariant,
    surfaceContainerHigh = DarkCobaltSurfaceHigh,
    outline = DarkCobaltOutline,
    outlineVariant = DarkCobaltSurfaceVariant,
    error = DarkCobaltTertiary,
    onError = DarkCobaltOnPrimary,
    errorContainer = DarkCobaltTertiaryContainer,
    onErrorContainer = DarkCobaltOnTertiaryContainer
)

private fun tealLight() = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = CobaltTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CobaltTertiaryContainer,
    onTertiaryContainer = CobaltOnTertiaryContainer,
    background = TealCanvas,
    onBackground = TealInk,
    surface = Color.White,
    onSurface = TealInk,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = TealMutedInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = TealCanvas,
    surfaceContainer = TealSurfaceVariant,
    surfaceContainerHigh = TealSurfaceHigh,
    outline = TealOutline,
    outlineVariant = TealOutlineVariant,
    error = CobaltTertiary,
    onError = Color.White,
    errorContainer = CobaltTertiaryContainer,
    onErrorContainer = CobaltOnTertiaryContainer
)

private fun tealDark() = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = DarkTealOnPrimary,
    primaryContainer = DarkTealPrimaryContainer,
    onPrimaryContainer = DarkTealOnPrimaryContainer,
    secondary = DarkSageSecondary,
    onSecondary = DarkTealOnPrimary,
    secondaryContainer = DarkTealSurfaceVariant,
    onSecondaryContainer = DarkTealOnPrimaryContainer,
    tertiary = DarkCobaltTertiary,
    onTertiary = DarkTealOnPrimary,
    tertiaryContainer = DarkCobaltTertiaryContainer,
    onTertiaryContainer = DarkCobaltOnTertiaryContainer,
    background = DarkTealCanvas,
    onBackground = DarkInk,
    surface = DarkTealSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkTealSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    surfaceContainerLowest = DarkTealCanvas,
    surfaceContainerLow = DarkTealSurface,
    surfaceContainer = DarkTealSurfaceVariant,
    surfaceContainerHigh = DarkTealSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkTealSurfaceVariant,
    error = DarkCobaltTertiary,
    onError = DarkTealOnPrimary,
    errorContainer = DarkCobaltTertiaryContainer,
    onErrorContainer = DarkCobaltOnTertiaryContainer
)

private fun linenLight() = lightColorScheme(
    primary = LinenPrimary,
    onPrimary = Color.White,
    primaryContainer = LinenPrimaryContainer,
    onPrimaryContainer = LinenOnPrimaryContainer,
    secondary = LinenSecondary,
    onSecondary = Color.White,
    secondaryContainer = LinenSecondaryContainer,
    onSecondaryContainer = LinenOnSecondaryContainer,
    tertiary = LinenTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LinenTertiaryContainer,
    onTertiaryContainer = LinenOnTertiaryContainer,
    background = LinenCanvas,
    onBackground = LinenInk,
    surface = Color.White,
    onSurface = LinenInk,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = LinenMutedInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LinenCanvas,
    surfaceContainer = LinenSurfaceVariant,
    surfaceContainerHigh = LinenSurfaceHigh,
    outline = LinenOutline,
    outlineVariant = LinenOutlineVariant,
    error = LinenPrimary,
    onError = Color.White,
    errorContainer = LinenPrimaryContainer,
    onErrorContainer = LinenOnPrimaryContainer
)

private fun linenDark() = darkColorScheme(
    primary = DarkLinenPrimary,
    onPrimary = DarkLinenOnPrimary,
    primaryContainer = DarkLinenPrimaryContainer,
    onPrimaryContainer = DarkLinenOnPrimaryContainer,
    secondary = Color(0xFFD7C2B8),
    onSecondary = DarkLinenOnPrimary,
    secondaryContainer = DarkLinenSurfaceVariant,
    onSecondaryContainer = DarkLinenOnPrimaryContainer,
    tertiary = DarkSagePrimary,
    onTertiary = DarkSageOnPrimary,
    tertiaryContainer = DarkSagePrimaryContainer,
    onTertiaryContainer = DarkSageOnPrimaryContainer,
    background = DarkLinenCanvas,
    onBackground = Color(0xFFF5ECE8),
    surface = DarkLinenSurface,
    onSurface = Color(0xFFF5ECE8),
    surfaceVariant = DarkLinenSurfaceVariant,
    onSurfaceVariant = Color(0xFFC8B6AD),
    surfaceContainerLowest = DarkLinenCanvas,
    surfaceContainerLow = DarkLinenSurface,
    surfaceContainer = DarkLinenSurfaceVariant,
    surfaceContainerHigh = DarkLinenSurfaceHigh,
    outline = Color(0xFF4D3E37),
    outlineVariant = DarkLinenSurfaceVariant,
    error = DarkLinenPrimary,
    onError = DarkLinenOnPrimary,
    errorContainer = DarkLinenPrimaryContainer,
    onErrorContainer = DarkLinenOnPrimaryContainer
)

private fun sageLight() = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnPrimary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    tertiary = TerracottaAccent,
    onTertiary = SageOnPrimary,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = CrispCanvas,
    onBackground = ForestInk,
    surface = PureCardSurface,
    onSurface = ForestInk,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = MutedForestInk,
    surfaceContainerLowest = PureCardSurface,
    surfaceContainerLow = CrispCanvas,
    surfaceContainer = SoftSurfaceVariant,
    surfaceContainerHigh = SageSurfaceHigh,
    outline = HairlineOutline,
    outlineVariant = SubtleOutlineVariant,
    error = TerracottaAccent,
    onError = SageOnPrimary,
    errorContainer = TerracottaContainer,
    onErrorContainer = OnTerracottaContainer
)

private fun sageDark() = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkSageOnPrimary,
    primaryContainer = DarkSagePrimaryContainer,
    onPrimaryContainer = DarkSageOnPrimaryContainer,
    secondary = DarkSageSecondary,
    onSecondary = DarkSageOnPrimary,
    secondaryContainer = DarkSageSecondaryContainer,
    onSecondaryContainer = DarkSageOnSecondaryContainer,
    tertiary = DarkTerracotta,
    onTertiary = DarkSageOnPrimary,
    tertiaryContainer = DarkTerracottaContainer,
    onTertiaryContainer = DarkOnTerracottaContainer,
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkCardSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    surfaceContainerLowest = DarkCanvas,
    surfaceContainerLow = DarkCardSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant,
    error = DarkTerracotta,
    onError = DarkSageOnPrimary,
    errorContainer = DarkTerracottaContainer,
    onErrorContainer = DarkOnTerracottaContainer
)

val StatinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StatinLogTheme(
    appearance: AppearanceConfig = AppearanceConfig(),
    content: @Composable () -> Unit
) {
    val isDark = when (appearance.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme: ColorScheme = when (appearance.palette) {
        AppPalette.COBALT_SLATE -> if (isDark) cobaltDark() else cobaltLight()
        AppPalette.OCEAN_TEAL -> if (isDark) tealDark() else tealLight()
        AppPalette.WARM_LINEN -> if (isDark) linenDark() else linenLight()
        AppPalette.CLINICAL_SAGE -> if (isDark) sageDark() else sageLight()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StatinTypography,
        shapes = StatinShapes,
        content = content
    )
}
