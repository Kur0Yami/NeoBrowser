package com.neobrowser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.webkit.*
import com.neobrowser.userscript.UserscriptManager

@SuppressLint("SetJavaScriptEnabled")
class NeoWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    private var isDesktopMode = false
    private val userscriptManager = UserscriptManager.getInstance(context)

    private val MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
    private val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    fun setup(
        activity: MainActivity,
        onPageStarted: (String) -> Unit,
        onPageFinished: (String) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onDownloadRequested: (String, String?, String?) -> Unit
    ) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            userAgentString = MOBILE_UA
        }

        // Enable cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@NeoWebView, true)
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                onPageStarted(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                injectUserscripts(url)
                onPageFinished(url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                // Title updated
            }
        }

        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            onDownloadRequested(url, contentDisposition, mimetype)
        }
    }

    private fun injectUserscripts(url: String) {
        val scripts = userscriptManager.getMatchingScripts(url)
        for (script in scripts) {
            if (script.enabled) {
                val js = buildUserscriptWrapper(script.code, script.runAt)
                evaluateJavascript(js, null)
            }
        }
    }

    private fun buildUserscriptWrapper(code: String, runAt: String): String {
        // Provide GM_ API compatibility layer
        return """
            (function() {
                // Tampermonkey/Greasemonkey GM_ API compatibility
                const GM_info = { version: '1.0', scriptHandler: 'NeoBrowser' };
                function GM_getValue(key, defaultValue) {
                    try { return JSON.parse(localStorage.getItem('GM_' + key)) ?? defaultValue; }
                    catch(e) { return defaultValue; }
                }
                function GM_setValue(key, value) {
                    localStorage.setItem('GM_' + key, JSON.stringify(value));
                }
                function GM_deleteValue(key) { localStorage.removeItem('GM_' + key); }
                function GM_listValues() {
                    return Object.keys(localStorage).filter(k => k.startsWith('GM_')).map(k => k.slice(3));
                }
                function GM_log(msg) { console.log('[UserScript]', msg); }
                function GM_addStyle(css) {
                    const style = document.createElement('style');
                    style.textContent = css;
                    document.head.appendChild(style);
                }
                function GM_xmlhttpRequest(details) {
                    const xhr = new XMLHttpRequest();
                    xhr.open(details.method || 'GET', details.url);
                    if (details.headers) {
                        Object.entries(details.headers).forEach(([k,v]) => xhr.setRequestHeader(k, v));
                    }
                    xhr.onload = () => details.onload?.({ responseText: xhr.responseText, status: xhr.status });
                    xhr.onerror = () => details.onerror?.({ error: 'Network error' });
                    xhr.send(details.data);
                }
                function GM_openInTab(url) { window.open(url, '_blank'); }
                function GM_setClipboard(text) { navigator.clipboard?.writeText(text); }
                
                // unsafeWindow
                const unsafeWindow = window;
                
                try {
                    $code
                } catch(e) {
                    console.error('[UserScript Error]', e);
                }
            })();
        """.trimIndent()
    }

    fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        settings.userAgentString = if (isDesktopMode) DESKTOP_UA else MOBILE_UA
        reload()
    }
}
