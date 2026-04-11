package com.neobrowser.browser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neobrowser.R
import com.neobrowser.tabs.Tab
import com.neobrowser.tabs.TabManager
import com.neobrowser.tabs.TabsOverviewActivity
import com.neobrowser.userscript.UserscriptManagerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: NeoWebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnTabs: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var tabCount: TextView
    private lateinit var toolbar: LinearLayout
    private lateinit var statusBarSpace: View

    private val tabManager = TabManager.instance
    var currentTabId: String = ""
    private var toolbarVisible = true
    private var fullscreenMode = "auto"

    private fun prefs() = getSharedPreferences(SettingsActivity.PREF, MODE_PRIVATE)
    private fun homepage() = prefs().getString(SettingsActivity.KEY_HOMEPAGE, "https://google.com") ?: "https://google.com"
    private fun newTabUrl() = when (prefs().getString(SettingsActivity.KEY_NEW_TAB, "home")) {
        "blank" -> "about:blank"
        "custom" -> prefs().getString("new_tab_custom_url", homepage()) ?: homepage()
        else -> homepage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fullscreenMode = prefs().getString(SettingsActivity.KEY_FULLSCREEN, "auto") ?: "auto"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        initViews()
        setupInsets()
        setupWebView()
        setupUrlBar()
        setupButtons()

        // Scroll-hide toolbar aktif untuk "auto" dan "fullscreen" (immersive)
        if (fullscreenMode == "auto" || fullscreenMode == "fullscreen") {
            setupScrollHide()
        }

        val startUrl = when (prefs().getString(SettingsActivity.KEY_START_PAGE, "last")) {
            "home"  -> homepage()
            "blank" -> "about:blank"
            else    -> intent?.dataString ?: homepage()
        }
        loadUrl(startUrl)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { _, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            statusBarSpace.layoutParams.height = sb
            statusBarSpace.requestLayout()
            insets
        }
        // Immersive: sembunyikan system bars saja, toolbar tetap bisa scroll-hide
        if (fullscreenMode == "fullscreen") {
            toolbar.post { applyImmersive() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (fullscreenMode == "fullscreen") applyImmersive()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && fullscreenMode == "fullscreen") applyImmersive()
    }

    @Suppress("DEPRECATION")
    private fun applyImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    private fun initViews() {
        webView        = findViewById(R.id.webview)
        urlBar         = findViewById(R.id.url_bar)
        progressBar    = findViewById(R.id.progress_bar)
        btnBack        = findViewById(R.id.btn_back)
        btnForward     = findViewById(R.id.btn_forward)
        btnTabs        = findViewById(R.id.btn_tabs)
        btnMenu        = findViewById(R.id.btn_menu)
        tabCount       = findViewById(R.id.tab_count)
        toolbar        = findViewById(R.id.toolbar)
        statusBarSpace = findViewById(R.id.status_bar_space)
    }

    // Toolbar hide/show on scroll – dipakai oleh BOTH "auto" and "fullscreen"
    private fun setupScrollHide() {
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 15 && toolbarVisible && scrollY > 80) hideToolbar()
            else if (dy < -15 && !toolbarVisible) showToolbar()
        }
    }

    private fun hideToolbar() {
        if (!toolbarVisible) return
        toolbarVisible = false
        toolbar.animate()
            .translationY(-toolbar.height.toFloat())
            .setDuration(220)
            .withEndAction { toolbar.visibility = View.GONE }
            .start()
    }

    fun showToolbar() {
        if (toolbarVisible) return
        toolbarVisible = true
        toolbar.visibility = View.VISIBLE
        toolbar.translationY = -toolbar.height.toFloat()
        toolbar.animate()
            .translationY(0f)
            .setDuration(220)
            .start()
    }

    private fun setupWebView() {
        webView.setup(this,
            onPageStarted    = { url -> urlBar.setText(url); progressBar.visibility = View.VISIBLE; updateNavButtons() },
            onPageFinished   = { url ->
                urlBar.setText(url); progressBar.visibility = View.GONE; updateNavButtons()
                tabManager.updateTab(this, currentTabId, url, webView.title ?: url)
                updateTabCount(); tabManager.saveThumb(currentTabId, webView)
            },
            onProgressChanged   = { p -> progressBar.progress = p },
            onDownloadRequested = { url, cd, mime -> handleDownload(url, cd, mime) }
        )

        // Restore last session atau buat tab baru
        tabManager.init(this)
        val lastTabId = prefs().getString("last_tab_id", null)
        val restoredTab = if (lastTabId != null) tabManager.getTab(lastTabId) else null

        if (tabManager.tabs.isEmpty()) {
            val tab = tabManager.createTab(this, homepage(), "New Tab")
            currentTabId = tab.id
            loadUrl(homepage())
        } else {
            val tab = restoredTab ?: tabManager.tabs.last()
            currentTabId = tab.id
            loadUrl(tab.url)
        }
        updateTabCount()
    }

    private fun setupUrlBar() {
        urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !toolbarVisible) showToolbar()
        }
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loadUrl(processInput(urlBar.text.toString().trim()))
                urlBar.clearFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(urlBar.windowToken, 0)
                true
            } else false
        }
    }

    private fun processInput(input: String) = when {
        input.startsWith("http://") || input.startsWith("https://") -> input
        input.contains(".") && !input.contains(" ") -> "https://$input"
        else -> "https://www.google.com/search?q=${Uri.encode(input)}"
    }

    private fun setupButtons() {
        btnBack.setOnClickListener    { if (webView.canGoBack()) webView.goBack() }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        btnTabs.setOnClickListener    {
            startActivityForResult(
                Intent(this, TabsOverviewActivity::class.java)
                    .putExtra("current_tab_id", currentTabId), REQUEST_TABS)
        }
        btnMenu.setOnClickListener { showMenu() }
    }

    private fun showMenu() {
        val popup = PopupMenu(this, btnMenu)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_new_tab     -> { newTab(newTabUrl()); true }
                R.id.menu_home        -> { loadUrl(homepage()); true }
                R.id.menu_refresh     -> { webView.reload(); true }
                R.id.menu_userscripts -> { startActivity(Intent(this, UserscriptManagerActivity::class.java)); true }
                R.id.menu_share       -> {
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, webView.url) }
                    startActivity(Intent.createChooser(i, "Share")); true
                }
                R.id.menu_desktop  -> { webView.toggleDesktopMode(); true }
                R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }
        popup.show()
    }

    fun loadUrl(url: String) { webView.loadUrl(url) }

    fun newTab(url: String) {
        val tab = tabManager.createTab(this, url, "New Tab")
        currentTabId = tab.id
        webView.loadUrl(url); updateTabCount()
    }

    fun switchToTab(tab: Tab) {
        currentTabId = tab.id
        prefs().edit().putString("last_tab_id", tab.id).apply()
        webView.loadUrl(tab.url); updateTabCount()
    }

    fun handleDownload(url: String, cd: String?, mime: String?) {
        val mode = prefs().getString(SettingsActivity.KEY_DL_MODE, "ask") ?: "ask"
        val uri  = Uri.parse(url)
        if (mode == "ask")    { startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Download with...")); return }
        if (mode == "system") { startActivity(Intent(Intent.ACTION_VIEW, uri)); return }
        val pkg = when (mode) {
            "1dm"    -> "idm.internet.download.manager.plus"
            "idm"    -> "idm.internet.download.manager"
            "adm"    -> "com.dv.adm"
            "custom" -> prefs().getString(SettingsActivity.KEY_DL_PKG, "") ?: ""
            else     -> ""
        }
        if (pkg.isNotEmpty()) {
            try { startActivity(Intent(Intent.ACTION_VIEW, uri).apply { setPackage(pkg); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); return }
            catch (e: Exception) { Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show() }
        }
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun updateNavButtons() {
        btnBack.alpha    = if (webView.canGoBack())    1f else 0.3f
        btnForward.alpha = if (webView.canGoForward()) 1f else 0.3f
    }

    private fun updateTabCount() {
        val c = tabManager.tabs.size
        tabCount.text = if (c > 99) "99+" else c.toString()
    }

    override fun onPause() {
        super.onPause()
        // Simpan tab aktif
        prefs().edit().putString("last_tab_id", currentTabId).apply()
        tabManager.saveTabs(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TABS && resultCode == RESULT_OK) {
            val action = data?.getStringExtra("action")
            if (action == "new_tab") { newTab(newTabUrl()); return }
            val tabId = data?.getStringExtra("selected_tab_id") ?: return
            switchToTab(tabManager.getTab(tabId) ?: return)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object { const val REQUEST_TABS = 1001 }
}
