package com.neobrowser.browser
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.webkit.*
import com.neobrowser.userscript.UserscriptManager

@SuppressLint("SetJavaScriptEnabled")
class NeoWebView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null): WebView(ctx, attrs) {

    private val sm = UserscriptManager.instance
    var onStarted:  ((String) -> Unit)? = null
    var onFinished: ((String) -> Unit)? = null
    var onProgress: ((Int) -> Unit)? = null
    var onDownload: ((String,String,String) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(onPageStarted:(String)->Unit, onPageFinished:(String)->Unit,
              onProgressChanged:(Int)->Unit, onDownloadRequested:(String,String,String)->Unit) {
        onStarted=onPageStarted; onFinished=onPageFinished
        onProgress=onProgressChanged; onDownload=onDownloadRequested
        with(settings) {
            javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true
            setSupportZoom(true); builtInZoomControls=true; displayZoomControls=false
            useWideViewPort=true; loadWithOverviewMode=true
            allowContentAccess=true; allowFileAccess=true
            mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode=WebSettings.LOAD_DEFAULT; mediaPlaybackRequiresUserGesture=false
        }
        webViewClient = object:WebViewClient() {
            override fun onPageStarted(v:WebView,url:String,f:Bitmap?) { onStarted?.invoke(url) }
            override fun onPageFinished(v:WebView,url:String) { inject(url); onFinished?.invoke(url) }
            override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest)=false
        }
        webChromeClient = object:WebChromeClient() {
            override fun onProgressChanged(v:WebView,p:Int) { onProgress?.invoke(p) }
        }
        setDownloadListener { url,_,cd,mime,_ -> onDownload?.invoke(url,cd?:"",mime?:"application/octet-stream") }
    }

    private fun inject(url:String) {
        sm.getMatchingScripts(url,"document-end").forEach { s ->
            val pfx = s.name.replace(Regex("[^A-Za-z0-9]"),"_")
            val shim = "(function(){'use strict';" +
                "const _p='__GM_"+pfx+"_';" +
                "const GM_getValue=(k,d)=>{try{return JSON.parse(localStorage.getItem(_p+k))??d}catch(e){return d}};" +
                "const GM_setValue=(k,v)=>localStorage.setItem(_p+k,JSON.stringify(v));" +
                "const GM_deleteValue=k=>localStorage.removeItem(_p+k);" +
                "const GM_addStyle=css=>{const s=document.createElement('style');s.textContent=css;document.head.appendChild(s)};" +
                "const GM_log=(...a)=>console.log('[GM:"+s.name+"]',...a);" +
                "const GM_openInTab=u=>window.open(u,'_blank');" +
                "const GM_setClipboard=t=>navigator.clipboard?.writeText(t);" +
                "const GM_xmlhttpRequest=d=>fetch(d.url,{method:d.method||'GET',headers:d.headers||{},body:d.data})" +
                ".then(r=>r.text()).then(t=>d.onload?.({responseText:t})).catch(e=>d.onerror?.({error:e}));" +
                s.code + "})();"
            evaluateJavascript(shim,null)
        }
    }

    fun setDesktopMode(on:Boolean) {
        settings.userAgentString = if(on)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
        else
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
        settings.useWideViewPort=on; settings.loadWithOverviewMode=on; reload()
    }
}
