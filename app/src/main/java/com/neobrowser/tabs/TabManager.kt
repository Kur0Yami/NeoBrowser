package com.neobrowser.tabs
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TabManager {
    val instance=this
    val tabs=mutableListOf<Tab>(); val groups=mutableListOf<TabGroup>()
    private val thumbs=LruCache<String,Bitmap>(20); private val gson=Gson()
    fun init(ctx:Context){
        val p=ctx.getSharedPreferences("neo_tabs",Context.MODE_PRIVATE)
        val tt=object:TypeToken<MutableList<Tab>>(){}.type
        tabs.clear();tabs.addAll(gson.fromJson(p.getString("tabs","[]"),tt)?:emptyList())
        val gt=object:TypeToken<MutableList<TabGroup>>(){}.type
        groups.clear();groups.addAll(gson.fromJson(p.getString("groups","[]"),gt)?:emptyList())
    }
    private fun save(ctx:Context){
        ctx.getSharedPreferences("neo_tabs",Context.MODE_PRIVATE).edit()
            .putString("tabs",gson.toJson(tabs)).putString("groups",gson.toJson(groups)).apply()
    }
    fun createTab(ctx:Context,url:String,title:String):Tab{
        val t=Tab(java.util.UUID.randomUUID().toString(),url,title);tabs.add(t);save(ctx);return t
    }
    fun getTab(id:String)=tabs.find{it.id==id}
    fun removeTab(ctx:Context,id:String){tabs.removeAll{it.id==id};thumbs.remove(id);save(ctx)}
    fun updateTab(ctx:Context,id:String,url:String,title:String){getTab(id)?.let{it.url=url;it.title=title;save(ctx)}}
    fun saveThumb(id:String,wv:WebView){try{
        val bm=Bitmap.createBitmap(wv.width.coerceAtLeast(1),wv.height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        wv.draw(Canvas(bm));thumbs.put(id,Bitmap.createScaledBitmap(bm,300,200,true))
    }catch(e:Exception){}}
    fun getThumb(id:String)=thumbs[id]
}
