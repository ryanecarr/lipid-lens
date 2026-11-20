package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppPalette(
    val key: String,
    val title: String,
    val subtitle: String,
    val previewPrimaryHex: Long,
    val previewContainerHex: Long,
    val previewAccentHex: Long
) {
    COBALT_SLATE(
        key = "cobalt_slate",
        title = "Cobalt & Slate",
        subtitle = "Crisp sapphire blue and cool porcelain",
        previewPrimaryHex = 0xFF1D549F,
        previewContainerHex = 0xFFE0ECFF,
        previewAccentHex = 0xFFD14D3A
    ),
    OCEAN_TEAL(
        key = "ocean_teal",
        title = "Ocean Teal",
        subtitle = "Deep lagoon teal and bright ice white",
        previewPrimaryHex = 0xFF0F766E,
        previewContainerHex = 0xFFD5F5F0,
        previewAccentHex = 0xFFD9534F
    ),
    WARM_LINEN(
        key = "warm_linen",
        title = "Warm Terracotta",
        subtitle = "Sunlit warm linen and rich clay",
        previewPrimaryHex = 0xFFA8442E,
        previewContainerHex = 0xFFFAE5DE,
        previewAccentHex = 0xFF3E6B5C
    ),
    CLINICAL_SAGE(
        key = "clinical_sage",
        title = "Clinical Sage",
        subtitle = "Soft botanical sage and crisp white",
        previewPrimaryHex = 0xFF2C6B5B,
        previewContainerHex = 0xFFDCEFE8,
        previewAccentHex = 0xFFC75C46
    );

    companion object {
        fun fromKey(key: String?): AppPalette =
            entries.firstOrNull { it.key == key } ?: COBALT_SLATE
    }
}

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class AppearanceConfig(
    val palette: AppPalette = AppPalette.COBALT_SLATE,
    val mode: ThemeMode = ThemeMode.SYSTEM
)

class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "statinlog_appearance",
        Context.MODE_PRIVATE
    )

    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<AppearanceConfig> = _config.asStateFlow()

    private fun readConfig(): AppearanceConfig = AppearanceConfig(
        palette = AppPalette.fromKey(prefs.getString(KEY_PALETTE, AppPalette.COBALT_SLATE.key)),
        mode = ThemeMode.fromKey(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.key))
    )

    fun setPalette(palette: AppPalette) {
        prefs.edit().putString(KEY_PALETTE, palette.key).apply()
        _config.value = _config.value.copy(palette = palette)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.key).apply()
        _config.value = _config.value.copy(mode = mode)
    }

    private companion object {
        const val KEY_PALETTE = "palette"
        const val KEY_MODE = "theme_mode"
    }
}
