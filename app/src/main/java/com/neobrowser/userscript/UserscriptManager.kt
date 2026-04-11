package com.neobrowser.userscript

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class Userscript(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String = "",
    var version: String = "1.0",
    var author: String = "",
    var matches: List<String> = listOf("*"),  // URL patterns
    var excludes: List<String> = listOf(),
    var code: String,
    var enabled: Boolean = true,
    var runAt: String = "document-end"  // document-start, document-end, document-idle
)

class UserscriptManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("userscripts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var scripts = mutableListOf<Userscript>()

    init {
        loadScripts()
        if (scripts.isEmpty()) {
            // Add sample script
            addScript(Userscript(
                name = "Dark Mode Everywhere",
                description = "Applies dark mode to all websites",
                matches = listOf("*"),
                code = """
                    // @name Dark Mode Everywhere
                    // @description Forces dark mode on all sites
                    GM_addStyle(`
                        html { filter: invert(1) hue-rotate(180deg) !important; }
                        img, video, canvas, iframe { filter: invert(1) hue-rotate(180deg) !important; }
                    `);
                """.trimIndent(),
                enabled = false
            ))
            addScript(Userscript(
                name = "Remove YouTube Ads",
                description = "Skips YouTube ads automatically",
                matches = listOf("*youtube.com/*"),
                code = """
                    // @name Remove YouTube Ads
                    // @match *youtube.com/*
                    setInterval(() => {
                        const skipBtn = document.querySelector('.ytp-skip-ad-button, .ytp-ad-skip-button');
                        if (skipBtn) skipBtn.click();
                        const adOverlay = document.querySelector('.ad-showing');
                        if (adOverlay) {
                            const video = document.querySelector('video');
                            if (video) video.currentTime = video.duration;
                        }
                    }, 500);
                """.trimIndent(),
                enabled = false
            ))
        }
    }

    private fun loadScripts() {
        val json = prefs.getString("scripts", null) ?: return
        val type = object : TypeToken<MutableList<Userscript>>() {}.type
        scripts = gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveScripts() {
        prefs.edit().putString("scripts", gson.toJson(scripts)).apply()
    }

    fun getAllScripts(): List<Userscript> = scripts.toList()

    fun addScript(script: Userscript) {
        scripts.add(script)
        saveScripts()
    }

    fun updateScript(script: Userscript) {
        val index = scripts.indexOfFirst { it.id == script.id }
        if (index >= 0) {
            scripts[index] = script
            saveScripts()
        }
    }

    fun deleteScript(id: String) {
        scripts.removeAll { it.id == id }
        saveScripts()
    }

    fun toggleScript(id: String) {
        scripts.find { it.id == id }?.let {
            it.enabled = !it.enabled
            saveScripts()
        }
    }

    fun getMatchingScripts(url: String): List<Userscript> {
        return scripts.filter { script ->
            script.enabled && script.matches.any { pattern -> matchesPattern(url, pattern) } &&
            script.excludes.none { pattern -> matchesPattern(url, pattern) }
        }
    }

    private fun matchesPattern(url: String, pattern: String): Boolean {
        if (pattern == "*") return true
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return try {
            Regex(regex).containsMatchIn(url)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile private var instance: UserscriptManager? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: UserscriptManager(context.applicationContext).also { instance = it }
        }
    }
}
