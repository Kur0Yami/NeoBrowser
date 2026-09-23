package com.neobrowser.browser

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.neobrowser.R

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREF = "neo_prefs"
        const val KEY_DL_MODE = "dl_mode"
        const val KEY_DL_PKG  = "dl_pkg"
        const val KEY_FULLSCREEN = "fullscreen"
        const val KEY_HOMEPAGE = "homepage"
        const val KEY_START_PAGE = "start_page"
        const val KEY_NEW_TAB = "new_tab_page"
        const val KEY_UA_MODE = "ua_mode"
        const val KEY_CUSTOM_UA = "custom_ua"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences(PREF, MODE_PRIVATE)

        // --- Homepage ---
        val homepageInput = findViewById<EditText>(R.id.input_homepage)
        homepageInput.setText(prefs.getString(KEY_HOMEPAGE, "https://google.com"))
        findViewById<Button>(R.id.btn_save_homepage).setOnClickListener {
            val v = homepageInput.text.toString().trim().let {
                if (it.isNotEmpty() && !it.startsWith("http")) "https://$it" else it
            }
            prefs.edit().putString(KEY_HOMEPAGE, v.ifEmpty { "https://google.com" }).apply()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }

        // --- Start Page ---
        val startSpinner = findViewById<Spinner>(R.id.spinner_start_page)
        val startOptions = arrayOf("Last visited page", "Homepage", "Blank page")
        val startValues  = arrayOf("last", "home", "blank")
        startSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, startOptions)
        val savedStart = prefs.getString(KEY_START_PAGE, "last")
        startSpinner.setSelection(startValues.indexOf(savedStart).coerceAtLeast(0))
        startSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString(KEY_START_PAGE, startValues[pos]).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // --- New Tab Page ---
        val newTabSpinner = findViewById<Spinner>(R.id.spinner_new_tab)
        val newTabOptions = arrayOf("Homepage", "Blank page", "Custom URL")
        val newTabValues  = arrayOf("home", "blank", "custom")
        newTabSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, newTabOptions)
        val savedNewTab = prefs.getString(KEY_NEW_TAB, "home")
        newTabSpinner.setSelection(newTabValues.indexOf(savedNewTab).coerceAtLeast(0))

        val customNewTabInput = findViewById<EditText>(R.id.input_new_tab_url)
        customNewTabInput.setText(prefs.getString("new_tab_custom_url", ""))
        customNewTabInput.visibility = if (savedNewTab == "custom") android.view.View.VISIBLE else android.view.View.GONE

        newTabSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString(KEY_NEW_TAB, newTabValues[pos]).apply()
                customNewTabInput.visibility = if (newTabValues[pos] == "custom") android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        findViewById<Button>(R.id.btn_save_new_tab).setOnClickListener {
            prefs.edit().putString("new_tab_custom_url", customNewTabInput.text.toString().trim()).apply()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }

        // --- Download Manager ---
        val dlSpinner = findViewById<Spinner>(R.id.spinner_dl_manager)
        val dlOptions = arrayOf("Ask every time", "1DM+", "IDM+", "ADM", "System default", "Custom package...")
        val dlValues  = arrayOf("ask", "1dm", "idm", "adm", "system", "custom")
        dlSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dlOptions)
        val savedDl = prefs.getString(KEY_DL_MODE, "ask")
        dlSpinner.setSelection(dlValues.indexOf(savedDl).coerceAtLeast(0))
        val customPkgInput = findViewById<EditText>(R.id.input_custom_pkg)
        customPkgInput.setText(prefs.getString(KEY_DL_PKG, ""))
        customPkgInput.visibility = if (savedDl == "custom") android.view.View.VISIBLE else android.view.View.GONE
        dlSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString(KEY_DL_MODE, dlValues[pos]).apply()
                customPkgInput.visibility = if (dlValues[pos] == "custom") android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        findViewById<Button>(R.id.btn_save_pkg).setOnClickListener {
            prefs.edit().putString(KEY_DL_PKG, customPkgInput.text.toString().trim()).apply()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }

        // --- Fullscreen / Display ---
        val fsSpinner = findViewById<Spinner>(R.id.spinner_fullscreen)
        val fsOptions = arrayOf("Normal (show status bar)", "Auto-hide toolbar on scroll", "Full immersive (hide all)")
        val fsValues  = arrayOf("normal", "auto", "fullscreen")
        fsSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fsOptions)
        val savedFs = prefs.getString(KEY_FULLSCREEN, "auto")
        fsSpinner.setSelection(fsValues.indexOf(savedFs).coerceAtLeast(0))
        fsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString(KEY_FULLSCREEN, fsValues[pos]).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // --- User Agent ---
        val uaSpinner = findViewById<Spinner>(R.id.spinner_ua_mode)
        val uaOptions = arrayOf("Mobile Chrome (default)", "Custom User-Agent")
        val uaValues  = arrayOf("default", "custom")
        uaSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uaOptions)
        val savedUaMode = prefs.getString(KEY_UA_MODE, "default")
        uaSpinner.setSelection(uaValues.indexOf(savedUaMode).coerceAtLeast(0))

        val customUaInput = findViewById<EditText>(R.id.input_custom_ua)
        val btnSaveUa = findViewById<Button>(R.id.btn_save_ua)
        customUaInput.setText(prefs.getString(KEY_CUSTOM_UA, ""))
        val uaExtrasVisible = if (savedUaMode == "custom") android.view.View.VISIBLE else android.view.View.GONE
        customUaInput.visibility = uaExtrasVisible
        btnSaveUa.visibility = uaExtrasVisible

        uaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString(KEY_UA_MODE, uaValues[pos]).apply()
                val vis = if (uaValues[pos] == "custom") android.view.View.VISIBLE else android.view.View.GONE
                customUaInput.visibility = vis
                btnSaveUa.visibility = vis
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        btnSaveUa.setOnClickListener {
            prefs.edit().putString(KEY_CUSTOM_UA, customUaInput.text.toString().trim()).apply()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }

        // --- Clear browsing data ---
        findViewById<Button>(R.id.btn_clear_data).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear browsing data?")
                .setMessage("This removes cookies, cache, and site storage (localStorage/IndexedDB). You'll be logged out of most sites. This cannot be undone.")
                .setPositiveButton("Clear") { _, _ -> clearBrowsingData() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun clearBrowsingData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        // WebView butuh instance aktif untuk membersihkan cache HTTP-nya sendiri
        val tempWebView = WebView(this)
        tempWebView.clearCache(true)
        tempWebView.clearHistory()
        tempWebView.clearFormData()
        tempWebView.destroy()
        Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}