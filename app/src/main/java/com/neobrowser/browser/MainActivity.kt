package com.neobrowser.browser
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import com.neobrowser.R
import com.neobrowser.tabs.*
import com.neobrowser.userscript.UserscriptManagerActivity

class MainActivity : AppCompatActivity() {
    lateinit var webView: NeoWebView
    private lateinit var urlBar:EditText
    private lateinit var progressBar:ProgressBar
    private lateinit var btnBack:View; private lateinit var btnFwd:View
    private lateinit var tabBadge:TextView
    private lateinit var toolbar:View; private lateinit var statusSpace:View
    private val tabs=TabManager.instance
    var currentTabId=""; private var toolbarShown=true
    companion object { const val REQ_TABS=1001 }
    private fun prefs()=getSharedPreferences(SettingsActivity.PREF,MODE_PRIVATE)
    private fun homepage()=prefs().getString(SettingsActivity.KEY_HOMEPAGE,"https://google.com")?:"https://google.com"

    override fun onCreate(state:Bundle?) {
        ThemeManager.apply(this); super.onCreate(state)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) window.setDecorFitsSystemWindows(false)
        setContentView(R.layout.activity_main); supportActionBar?.hide()
        webView=findViewById(R.id.webview); urlBar=findViewById(R.id.url_bar)
        progressBar=findViewById(R.id.progress_bar); btnBack=findViewById(R.id.btn_back)
        btnFwd=findViewById(R.id.btn_forward); tabBadge=findViewById(R.id.tab_count_badge)
        toolbar=findViewById(R.id.toolbar); statusSpace=findViewById(R.id.status_bar_space)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar){_,ins->
            statusSpace.updateLayoutParams{height=ins.getInsets(WindowInsetsCompat.Type.statusBars()).top};ins}
        setupWebView(); setupUrlBar(); setupButtons(); setupBottomNav(); setupScrollHide()
    }

    private fun setupWebView() {
        tabs.init(this)
        webView.setup(
            onPageStarted={url->urlBar.setText(url);progressBar.visibility=View.VISIBLE;updateNav()},
            onPageFinished={url->urlBar.setText(url);progressBar.visibility=View.GONE;updateNav()
                tabs.updateTab(this,currentTabId,url,webView.title?:url)
                tabs.saveThumb(currentTabId,webView);updateBadge()},
            onProgressChanged={p->progressBar.progress=p},
            onDownloadRequested={url,cd,mime->handleDownload(url,cd,mime)}
        )
        val last=prefs().getString("last_tab_id",null)?.let{tabs.getTab(it)}
        if(tabs.tabs.isEmpty()){val t=tabs.createTab(this,homepage(),"New Tab");currentTabId=t.id;loadUrl(homepage())}
        else{val t=last?:tabs.tabs.last();currentTabId=t.id;loadUrl(t.url)}
        updateBadge()
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener{_,id,ev->
            if(id==EditorInfo.IME_ACTION_GO||ev?.keyCode==KeyEvent.KEYCODE_ENTER&&ev.action==KeyEvent.ACTION_DOWN){
                loadUrl(process(urlBar.text.toString().trim()))
                urlBar.clearFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(urlBar.windowToken,0)
                true
            } else false
        }
    }

    private fun process(q:String)=when{
        q.startsWith("http://")||q.startsWith("https://")->q
        q.contains(".")&&!q.contains(" ")->"https://\$q"
        else->"https://www.google.com/search?q="+Uri.encode(q)
    }

    private fun setupButtons() {
        btnBack.setOnClickListener{if(webView.canGoBack())webView.goBack()}
        btnFwd.setOnClickListener{if(webView.canGoForward())webView.goForward()}
        findViewById<View>(R.id.btn_tabs).setOnClickListener{openTabs()}
        findViewById<View>(R.id.btn_menu).setOnClickListener{showMenu()}
    }

    private fun setupBottomNav() {
        setNavActive(0)
        findViewById<View>(R.id.nav_home).setOnClickListener{loadUrl(homepage());setNavActive(0)}
        findViewById<View>(R.id.nav_tabs).setOnClickListener{openTabs();setNavActive(1)}
        findViewById<View>(R.id.nav_bookmarks).setOnClickListener{setNavActive(2)}
        findViewById<View>(R.id.nav_settings).setOnClickListener{
            startActivity(Intent(this,SettingsActivity::class.java));setNavActive(3)}
    }

    private fun setNavActive(i:Int) {
        val ids=listOf(R.id.nav_home_icon,R.id.nav_tabs_icon,R.id.nav_bookmarks_icon,R.id.nav_settings_icon)
        val acc=ThemeManager.accent(this);val ico=ThemeManager.icon(this)
        ids.forEachIndexed{idx,id->findViewById<ImageView>(id).setColorFilter(if(idx==i)acc else ico)}
    }

    private fun setupScrollHide() {
        webView.setOnScrollChangeListener{_,_,sy,_,oy->
            val dy=sy-oy
            if(dy>12&&toolbarShown&&sy>60)hideToolbar()
            else if(dy<-12&&!toolbarShown)showToolbar()
        }
    }

    private fun hideToolbar(){if(!toolbarShown)return;toolbarShown=false
        toolbar.animate().translationY(-toolbar.height.toFloat()).setDuration(200)
            .withEndAction{toolbar.visibility=View.GONE}.start()}
    fun showToolbar(){if(toolbarShown)return;toolbarShown=true
        toolbar.visibility=View.VISIBLE;toolbar.translationY=-toolbar.height.toFloat()
        toolbar.animate().translationY(0f).setDuration(200).start()}

    fun loadUrl(url:String){webView.loadUrl(url)}
    private fun updateNav(){btnBack.alpha=if(webView.canGoBack())1f else 0.3f;btnFwd.alpha=if(webView.canGoForward())1f else 0.3f}
    fun updateBadge(){tabBadge.text=tabs.tabs.size.toString()}

    private fun openTabs() {
        tabs.updateTab(this,currentTabId,webView.url?:"",webView.title?:"")
        tabs.saveThumb(currentTabId,webView)
        startActivityForResult(Intent(this,TabsOverviewActivity::class.java)
            .putExtra("current_tab_id",currentTabId),REQ_TABS)
    }

    private fun showMenu() {
        val popup=android.widget.PopupMenu(this,findViewById(R.id.btn_menu))
        popup.menuInflater.inflate(R.menu.browser_menu,popup.menu)
        popup.setOnMenuItemClickListener{
            when(it.itemId){
                R.id.menu_new_tab->{ val t=tabs.createTab(this,homepage(),"New Tab")
                    currentTabId=t.id;loadUrl(homepage());updateBadge();setNavActive(0);true}
                R.id.menu_refresh->{webView.reload();true}
                R.id.menu_home->{loadUrl(homepage());true}
                R.id.menu_desktop->{ val on=webView.settings.userAgentString.contains("Mobile").not()
                    webView.setDesktopMode(!on);true}
                R.id.menu_share->{ startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,webView.url)},
                    "Share"));true}
                R.id.menu_userscripts->{startActivity(Intent(this,UserscriptManagerActivity::class.java));true}
                R.id.menu_settings->{startActivity(Intent(this,SettingsActivity::class.java));true}
                else->false
            }
        }
        popup.show()
    }

    private fun handleDownload(url:String,cd:String,mime:String) {
        DownloadHelper.handle(this,url,cd,mime,
            prefs().getString(SettingsActivity.KEY_DL_MANAGER,"ask")?:"ask",
            prefs().getString("dl_custom_pkg","")?:"")
    }

    @Deprecated("Deprecated")
    override fun onActivityResult(req:Int,res:Int,data:Intent?) {
        super.onActivityResult(req,res,data)
        if(req==REQ_TABS&&res==RESULT_OK){
            val id=data?.getStringExtra("selected_tab_id")?:return
            val url=data.getStringExtra("selected_tab_url")?:return
            currentTabId=id;loadUrl(url);updateBadge();setNavActive(0)
        }
    }

    override fun onBackPressed(){if(webView.canGoBack())webView.goBack() else super.onBackPressed()}
    override fun onPause(){
        super.onPause()
        prefs().edit().putString("last_tab_id",currentTabId).apply()
        tabs.updateTab(this,currentTabId,webView.url?:"",webView.title?:"")
    }
}
