package com.chaomixian.vflow.ui.common

import android.content.Context
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalContext
import com.chaomixian.vflow.R

/**
 * 主题工具类
 * 提供统一的主题获取逻辑，供 Activity、Service、Manager 等使用
 * 支持 View 系统和 Compose 系统
 */
object ThemeUtils {

    private const val PREFS_NAME = "vFlowPrefs"
    private const val KEY_DYNAMIC_COLOR_ENABLED = "dynamicColorEnabled"
    const val KEY_COLORFUL_WORKFLOW_CARDS_ENABLED = "colorfulWorkflowCardsEnabled"

    /**
     * 获取主题资源 ID
     * @param context 上下文
     * @param transparent 是否使用透明主题（用于悬浮窗等场景）
     * @return 主题资源 ID
     */
    @JvmStatic
    fun getThemeResId(context: Context, transparent: Boolean = false): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useDynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, false)

        return when {
            transparent && useDynamicColor -> R.style.Theme_vFlow_Transparent_Dynamic
            transparent && !useDynamicColor -> R.style.Theme_vFlow_Transparent_Default
            !transparent && useDynamicColor -> R.style.Theme_vFlow_Dynamic
            else -> R.style.Theme_vFlow
        }
    }

    /**
     * 创建带主题的 ContextThemeWrapper
     * @param context 原始上下文
     * @param transparent 是否使用透明主题
     * @return 包装了主题的 Context
     */
    @JvmStatic
    fun createThemedContext(context: Context, transparent: Boolean = false): ContextThemeWrapper {
        val themeResId = getThemeResId(context, transparent)
        return ContextThemeWrapper(context, themeResId)
    }

    /**
     * 检查是否启用了动态取色
     * @param context 上下文
     * @return 是否启用动态取色
     */
    @JvmStatic
    fun isDynamicColorEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, false)
    }

    @JvmStatic
    fun isColorfulWorkflowCardsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COLORFUL_WORKFLOW_CARDS_ENABLED, false)
    }

    /**
     * 获取 Compose Material3 颜色方案
     * 支持：
     * 1. 动态取色（Material You）
     * 2. 深色模式自动切换
     * 3. 降级到默认配色
     *
     * @param darkTheme 是否使用深色主题（null 表示自动跟随系统）
     * @return ColorScheme 实例
     */
    @Composable
    fun getAppColorScheme(darkTheme: Boolean? = null): ColorScheme {
        val context = LocalContext.current
        val useDynamicColor = isDynamicColorEnabled(context)
        val isDarkTheme = darkTheme ?: isSystemInDarkTheme()

        return when {
            // Android 12+ 支持动态取色
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            // 降级到默认配色
            isDarkTheme -> darkAppColorSchemeFromResources()
            else -> lightAppColorSchemeFromResources()
        }
    }

    @Composable
    private fun lightAppColorSchemeFromResources(): ColorScheme = lightColorScheme(
        primary = colorResource(R.color.md_theme_light_primary),
        onPrimary = colorResource(R.color.md_theme_light_onPrimary),
        primaryContainer = colorResource(R.color.md_theme_light_primaryContainer),
        onPrimaryContainer = colorResource(R.color.md_theme_light_onPrimaryContainer),
        secondary = colorResource(R.color.md_theme_light_secondary),
        onSecondary = colorResource(R.color.md_theme_light_onSecondary),
        secondaryContainer = colorResource(R.color.md_theme_light_secondaryContainer),
        onSecondaryContainer = colorResource(R.color.md_theme_light_onSecondaryContainer),
        tertiary = colorResource(R.color.md_theme_light_tertiary),
        onTertiary = colorResource(R.color.md_theme_light_onTertiary),
        tertiaryContainer = colorResource(R.color.md_theme_light_tertiaryContainer),
        onTertiaryContainer = colorResource(R.color.md_theme_light_onTertiaryContainer),
        error = colorResource(R.color.md_theme_light_error),
        errorContainer = colorResource(R.color.md_theme_light_errorContainer),
        onError = colorResource(R.color.md_theme_light_onError),
        onErrorContainer = colorResource(R.color.md_theme_light_onErrorContainer),
        background = colorResource(R.color.md_theme_light_background),
        onBackground = colorResource(R.color.md_theme_light_onBackground),
        surface = colorResource(R.color.md_theme_light_surface),
        onSurface = colorResource(R.color.md_theme_light_onSurface),
        surfaceVariant = colorResource(R.color.md_theme_light_surfaceVariant),
        onSurfaceVariant = colorResource(R.color.md_theme_light_onSurfaceVariant),
        outline = colorResource(R.color.md_theme_light_outline),
        outlineVariant = colorResource(R.color.md_theme_light_outlineVariant),
        scrim = colorResource(R.color.md_theme_light_scrim),
        inverseSurface = colorResource(R.color.md_theme_light_inverseSurface),
        inverseOnSurface = colorResource(R.color.md_theme_light_inverseOnSurface),
        inversePrimary = colorResource(R.color.md_theme_light_inversePrimary),
        surfaceDim = colorResource(R.color.md_theme_light_surfaceDim),
        surfaceBright = colorResource(R.color.md_theme_light_surfaceBright),
        surfaceContainerLowest = colorResource(R.color.md_theme_light_surfaceContainerLowest),
        surfaceContainerLow = colorResource(R.color.md_theme_light_surfaceContainerLow),
        surfaceContainer = colorResource(R.color.md_theme_light_surfaceContainer),
        surfaceContainerHigh = colorResource(R.color.md_theme_light_surfaceContainerHigh),
        surfaceContainerHighest = colorResource(R.color.md_theme_light_surfaceContainerHighest)
    )

    @Composable
    private fun darkAppColorSchemeFromResources(): ColorScheme = darkColorScheme(
        primary = colorResource(R.color.md_theme_dark_primary),
        onPrimary = colorResource(R.color.md_theme_dark_onPrimary),
        primaryContainer = colorResource(R.color.md_theme_dark_primaryContainer),
        onPrimaryContainer = colorResource(R.color.md_theme_dark_onPrimaryContainer),
        secondary = colorResource(R.color.md_theme_dark_secondary),
        onSecondary = colorResource(R.color.md_theme_dark_onSecondary),
        secondaryContainer = colorResource(R.color.md_theme_dark_secondaryContainer),
        onSecondaryContainer = colorResource(R.color.md_theme_dark_onSecondaryContainer),
        tertiary = colorResource(R.color.md_theme_dark_tertiary),
        onTertiary = colorResource(R.color.md_theme_dark_onTertiary),
        tertiaryContainer = colorResource(R.color.md_theme_dark_tertiaryContainer),
        onTertiaryContainer = colorResource(R.color.md_theme_dark_onTertiaryContainer),
        error = colorResource(R.color.md_theme_dark_error),
        errorContainer = colorResource(R.color.md_theme_dark_errorContainer),
        onError = colorResource(R.color.md_theme_dark_onError),
        onErrorContainer = colorResource(R.color.md_theme_dark_onErrorContainer),
        background = colorResource(R.color.md_theme_dark_background),
        onBackground = colorResource(R.color.md_theme_dark_onBackground),
        surface = colorResource(R.color.md_theme_dark_surface),
        onSurface = colorResource(R.color.md_theme_dark_onSurface),
        surfaceVariant = colorResource(R.color.md_theme_dark_surfaceVariant),
        onSurfaceVariant = colorResource(R.color.md_theme_dark_onSurfaceVariant),
        outline = colorResource(R.color.md_theme_dark_outline),
        outlineVariant = colorResource(R.color.md_theme_dark_outlineVariant),
        scrim = colorResource(R.color.md_theme_dark_scrim),
        inverseSurface = colorResource(R.color.md_theme_dark_inverseSurface),
        inverseOnSurface = colorResource(R.color.md_theme_dark_inverseOnSurface),
        inversePrimary = colorResource(R.color.md_theme_dark_inversePrimary),
        surfaceDim = colorResource(R.color.md_theme_dark_surfaceDim),
        surfaceBright = colorResource(R.color.md_theme_dark_surfaceBright),
        surfaceContainerLowest = colorResource(R.color.md_theme_dark_surfaceContainerLowest),
        surfaceContainerLow = colorResource(R.color.md_theme_dark_surfaceContainerLow),
        surfaceContainer = colorResource(R.color.md_theme_dark_surfaceContainer),
        surfaceContainerHigh = colorResource(R.color.md_theme_dark_surfaceContainerHigh),
        surfaceContainerHighest = colorResource(R.color.md_theme_dark_surfaceContainerHighest)
    )
}
