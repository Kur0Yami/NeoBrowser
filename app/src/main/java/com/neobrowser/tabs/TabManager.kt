package com.neobrowser.tabs

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    var url: String,
    var title: String,
    var groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var color: Int = 0xFFBB86FC.toInt()
)

class TabManager private constructor() {

    val tabs   = mutableListOf<Tab>()
    val groups = mutableListOf<TabGroup>()
    private val thumbCache = LruCache<String, Bitmap>(20)
    private val gson = Gson()
    private var initialized = false

    /** Muat data dari SharedPreferences – panggil sekali di onCreate */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val tabsJson   = prefs.getString(KEY_TABS,   null)
        val groupsJson = prefs.getString(KEY_GROUPS, null)
        if (tabsJson != null) {
            val type = object : TypeToken<MutableList<Tab>>() {}.type
            val loaded: MutableList<Tab>? = gson.fromJson(tabsJson, type)
            if (loaded != null) { tabs.clear(); tabs.addAll(loaded) }
        }
        if (groupsJson != null) {
            val type = object : TypeToken<MutableList<TabGroup>>() {}.type
            val loaded: MutableList<TabGroup>? = gson.fromJson(groupsJson, type)
            if (loaded != null) { groups.clear(); groups.addAll(loaded) }
        }
    }

    /** Simpan semua tab & group ke SharedPreferences */
    fun saveTabs(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_TABS,   gson.toJson(tabs))
            .putString(KEY_GROUPS, gson.toJson(groups))
            .apply()
    }

    fun createTab(context: Context, url: String, title: String, groupId: String? = null): Tab {
        val tab = Tab(url = url, title = title, groupId = groupId)
        tabs.add(tab)
        saveTabs(context)
        return tab
    }

    fun updateTab(context: Context, id: String, url: String, title: String) {
        tabs.find { it.id == id }?.apply { this.url = url; this.title = title }
        saveTabs(context)
    }

    fun closeTab(id: String) {
        tabs.removeAll { it.id == id }
        thumbCache.remove(id)
    }

    fun closeTabAndSave(context: Context, id: String) {
        closeTab(id)
        saveTabs(context)
    }

    fun getTab(id: String) = tabs.find { it.id == id }

    fun saveThumb(tabId: String, webView: WebView) {
        try {
            webView.isDrawingCacheEnabled = true
            val bmp = webView.drawingCache
            if (bmp != null) {
                val scaled = Bitmap.createScaledBitmap(bmp, 300, 200, true)
                thumbCache.put(tabId, scaled)
            }
            webView.isDrawingCacheEnabled = false
        } catch (e: Exception) {}
    }

    fun getThumb(tabId: String): Bitmap? = thumbCache.get(tabId)

    fun createGroup(name: String, color: Int = 0xFFBB86FC.toInt()): TabGroup {
        val g = TabGroup(name = name, color = color)
        groups.add(g)
        return g
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        tabs.find { it.id == tabId }?.groupId = groupId
    }

    fun deleteGroup(groupId: String) {
        groups.removeAll { it.id == groupId }
        tabs.filter { it.groupId == groupId }.forEach { it.groupId = null }
    }

    companion object {
        val instance: TabManager by lazy { TabManager() }
        private const val PREF       = "neo_tabs"
        private const val KEY_TABS   = "tabs"
        private const val KEY_GROUPS = "groups"
    }
}
