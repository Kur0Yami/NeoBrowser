package com.neobrowser.browser
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.neobrowser.R

class SettingsActivity : AppCompatActivity() {
    companion object {
        const val PREF="neo_prefs"; const val KEY_HOMEPAGE="homepage"
        const val KEY_START_PAGE="start_page"; const val KEY_NEW_TAB="new_tab"
        const val KEY_THEME="theme"; const val KEY_FULLSCREEN="fullscreen"
        const val KEY_DL_MANAGER="dl_manager"
    }
    private fun prefs()=getSharedPreferences(PREF,MODE_PRIVATE)
    override fun onCreate(s:Bundle?) {
        ThemeManager.apply(this); super.onCreate(s)
        setContentView(R.layout.activity_settings)
        supportActionBar?.apply{title="Settings";setDisplayHomeAsUpEnabled(true)}
        val p=prefs()
        val etHome=findViewById<EditText>(R.id.input_homepage)
        etHome.setText(p.getString(KEY_HOMEPAGE,"https://google.com"))
        findViewById<android.widget.Button>(R.id.btn_save_homepage).setOnClickListener {
            p.edit().putString(KEY_HOMEPAGE,etHome.text.toString().trim()).apply()
            Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show()
        }
        fun sp(id:Int,items:List<String>,cur:String,fn:(String)->Unit) {
            val v=findViewById<Spinner>(id)
            v.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,items)
            v.setSelection(items.indexOfFirst{it==cur}.coerceAtLeast(0))
            v.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
                var first=true
                override fun onItemSelected(p:AdapterView<*>,vw:android.view.View?,pos:Int,id:Long){
                    if(first){first=false;return}; fn(items[pos]) }
                override fun onNothingSelected(p:AdapterView<*>){}
            }
        }
        sp(R.id.spinner_start_page,listOf("Last visited","Homepage","Blank"),
           when(p.getString(KEY_START_PAGE,"last")){"home"->"Homepage";"blank"->"Blank";else->"Last visited"})
           { s->p.edit().putString(KEY_START_PAGE,when(s){"Homepage"->"home";"Blank"->"blank";else->"last"}).apply() }
        sp(R.id.spinner_theme,listOf("System default","Dark","Light"),
           when(p.getString(KEY_THEME,"system")){"dark"->"Dark";"light"->"Light";else->"System default"})
           { s->val v=when(s){"Dark"->"dark";"Light"->"light";else->"system"}
             p.edit().putString(KEY_THEME,v).apply();ThemeManager.apply(this);recreate() }
        sp(R.id.spinner_fullscreen,listOf("Normal","Auto-hide toolbar","Full immersive"),
           when(p.getString(KEY_FULLSCREEN,"auto")){"auto"->"Auto-hide toolbar";"fullscreen"->"Full immersive";else->"Normal"})
           { s->p.edit().putString(KEY_FULLSCREEN,when(s){"Auto-hide toolbar"->"auto";"Full immersive"->"fullscreen";else->"normal"}).apply() }
        sp(R.id.spinner_dl_manager,listOf("Ask every time","1DM+","IDM+","ADM","System","Custom"),
           p.getString(KEY_DL_MANAGER,"ask")?.replaceFirstChar{it.uppercase()}?:"Ask")
           { s->p.edit().putString(KEY_DL_MANAGER,s.lowercase().replace(" ","")).apply() }
    }
    override fun onSupportNavigateUp():Boolean{onBackPressedDispatcher.onBackPressed();return true}
}
