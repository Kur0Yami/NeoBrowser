package com.neobrowser.userscript
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UserscriptManager {
    val instance=this; private val gson=Gson(); private val scripts=mutableListOf<Userscript>()
    fun init(ctx:Context){
        val p=ctx.getSharedPreferences("neo_scripts",Context.MODE_PRIVATE)
        val t=object:TypeToken<MutableList<Userscript>>(){}.type
        scripts.clear();scripts.addAll(gson.fromJson(p.getString("list","[]"),t)?:emptyList())
        if(scripts.isEmpty())seed(ctx)
    }
    fun getAll()=scripts.toList()
    fun getMatchingScripts(url:String,runAt:String)=scripts.filter{s->
        s.enabled&&s.runAt==runAt&&match(url,s.matches)&&!match(url,s.excludes)}
    fun add(ctx:Context,s:Userscript){scripts.add(s);save(ctx)}
    fun update(ctx:Context,s:Userscript){val i=scripts.indexOfFirst{it.id==s.id};if(i>=0){scripts[i]=s;save(ctx)}}
    fun remove(ctx:Context,id:String){scripts.removeAll{it.id==id};save(ctx)}
    fun toggle(ctx:Context,id:String){val i=scripts.indexOfFirst{it.id==id};if(i>=0){scripts[i]=scripts[i].copy(enabled=!scripts[i].enabled);save(ctx)}}
    private fun save(ctx:Context)=ctx.getSharedPreferences("neo_scripts",Context.MODE_PRIVATE).edit().putString("list",gson.toJson(scripts)).apply()
    private fun match(url:String,pats:List<String>):Boolean{
        if(pats.isEmpty())return false
        return pats.any{p->if(p=="*"||p=="*://*/*")true else Regex(p.replace(".","\\.").replace("*",".*")).containsMatchIn(url)}
    }
    private fun seed(ctx:Context){
        val dm="(function(){const css='html{filter:invert(1) hue-rotate(180deg)!important}img,video{filter:invert(1) hue-rotate(180deg)!important}';" +
               "const s=document.createElement('style');s.textContent=css;document.head.appendChild(s)})();"
        add(ctx,Userscript(name="Dark Mode Everywhere",description="Force dark on all sites",matches=listOf("*"),code=dm))
        val yt="setInterval(()=>{const b=document.querySelector('.ytp-ad-skip-button,.ytp-skip-ad-button');if(b)b.click()},500);"
        add(ctx,Userscript(name="Skip YouTube Ads",description="Auto-skip ads",matches=listOf("*youtube.com/*"),enabled=false,code=yt))
    }
}
