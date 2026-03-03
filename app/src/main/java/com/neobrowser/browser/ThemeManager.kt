package com.neobrowser.browser
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

enum class AppTheme(val value: String) { SYSTEM("system"), DARK("dark"), LIGHT("light") }

object ThemeManager {
    fun apply(context: Context) {
        val p = context.getSharedPreferences("neo_prefs", Context.MODE_PRIVATE)
        val t = AppTheme.values().find { it.value == p.getString("theme","system") } ?: AppTheme.SYSTEM
        AppCompatDelegate.setDefaultNightMode(when(t) {
            AppTheme.DARK   -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.LIGHT  -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }
    fun isDark(ctx: Context) =
        (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    fun accent(ctx: Context) = if(isDark(ctx)) 0xFFC594FF.toInt() else 0xFF592ACB.toInt()
    fun icon(ctx: Context)   = if(isDark(ctx)) 0xFFCFCFD8.toInt() else 0xFF5B5B66.toInt()
}
