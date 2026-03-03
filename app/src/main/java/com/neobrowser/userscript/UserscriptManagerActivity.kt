package com.neobrowser.userscript
import android.os.Bundle; import android.view.*; import android.widget.*
import androidx.appcompat.app.AlertDialog; import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.google.android.material.chip.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.neobrowser.R; import com.neobrowser.browser.ThemeManager

class UserscriptManagerActivity : AppCompatActivity() {
    private val mgr=UserscriptManager.instance; private lateinit var adapter:ScriptAdapter
    override fun onCreate(s:Bundle?){
        ThemeManager.apply(this); super.onCreate(s)
        setContentView(R.layout.activity_userscript_manager)
        supportActionBar?.apply{title="Userscripts";setDisplayHomeAsUpEnabled(true)}
        mgr.init(this)
        val rv=findViewById<RecyclerView>(R.id.scripts_recycler)
        rv.layoutManager=LinearLayoutManager(this)
        adapter=ScriptAdapter({s->mgr.toggle(this,s.id);refresh()},{s->edit(s)},{s->
            AlertDialog.Builder(this).setTitle("Delete '\${s.name}'?")
                .setPositiveButton("Delete"){_,_->mgr.remove(this,s.id);refresh()}
                .setNegativeButton("Cancel",null).show()})
        rv.adapter=adapter; refresh()
        findViewById<FloatingActionButton>(R.id.fab_add_script).setOnClickListener{edit(null)}
    }
    private fun refresh(){adapter.update(mgr.getAll())}
    private fun edit(script:Userscript?){
        val v=layoutInflater.inflate(R.layout.dialog_edit_script,null)
        val en=v.findViewById<EditText>(R.id.input_name)
        val ed=v.findViewById<EditText>(R.id.input_description)
        val em=v.findViewById<EditText>(R.id.input_matches)
        val ec=v.findViewById<EditText>(R.id.input_code)
        val sp=v.findViewById<Spinner>(R.id.spinner_run_at)
        val opts=listOf("document-start","document-end","document-idle")
        sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,opts)
        script?.let{en.setText(it.name);ed.setText(it.description);em.setText(it.matches.joinToString(","));ec.setText(it.code);sp.setSelection(opts.indexOf(it.runAt).coerceAtLeast(0))}
        AlertDialog.Builder(this).setTitle(if(script==null)"Add Script"else"Edit Script").setView(v)
            .setPositiveButton("Save"){_,_->
                val ns=Userscript(id=script?.id?:java.util.UUID.randomUUID().toString(),
                    name=en.text.toString().trim().ifEmpty{"Untitled"},description=ed.text.toString(),
                    matches=em.text.toString().split(",").map{it.trim()}.filter{it.isNotEmpty()},
                    runAt=opts[sp.selectedItemPosition],code=ec.text.toString(),enabled=script?.enabled?:true)
                if(script==null)mgr.add(this,ns)else mgr.update(this,ns);refresh()
            }.setNegativeButton("Cancel",null).show()
    }
    override fun onSupportNavigateUp():Boolean{onBackPressedDispatcher.onBackPressed();return true}
}

class ScriptAdapter(private val onToggle:(Userscript)->Unit,private val onEdit:(Userscript)->Unit,private val onDelete:(Userscript)->Unit)
    :RecyclerView.Adapter<ScriptAdapter.VH>(){
    private var list=listOf<Userscript>(); fun update(l:List<Userscript>){list=l;notifyDataSetChanged()}
    inner class VH(v:View):RecyclerView.ViewHolder(v){
        val name=v.findViewById<TextView>(R.id.script_name); val desc=v.findViewById<TextView>(R.id.script_desc)
        val tog=v.findViewById<Switch>(R.id.script_toggle); val chips=v.findViewById<ChipGroup>(R.id.chip_group_matches)
        val edit=v.findViewById<View>(R.id.btn_edit_script); val del=v.findViewById<View>(R.id.btn_delete_script)
    }
    override fun onCreateViewHolder(p:ViewGroup,t:Int)=VH(LayoutInflater.from(p.context).inflate(R.layout.item_userscript,p,false))
    override fun getItemCount()=list.size
    override fun onBindViewHolder(h:VH,pos:Int){
        val s=list[pos]; h.name.text=s.name; h.desc.text=s.description.ifEmpty{s.runAt}
        h.tog.isChecked=s.enabled; h.tog.setOnCheckedChangeListener(null); h.tog.setOnCheckedChangeListener{_,_->onToggle(s)}
        h.chips.removeAllViews(); s.matches.take(3).forEach{m->h.chips.addView(Chip(h.itemView.context).apply{text=m;isCheckable=false})}
        h.edit.setOnClickListener{onEdit(s)}; h.del.setOnClickListener{onDelete(s)}
    }
}
