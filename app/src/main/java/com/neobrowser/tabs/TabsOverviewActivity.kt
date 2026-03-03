package com.neobrowser.tabs
import android.app.Activity; import android.content.Intent; import android.os.Bundle
import android.view.*; import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.google.android.material.chip.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.neobrowser.R; import com.neobrowser.browser.ThemeManager

class TabsOverviewActivity : AppCompatActivity() {
    private val mgr=TabManager.instance
    private lateinit var rv:RecyclerView; private lateinit var adapter:TabAdapter
    private var currentTabId=""; private var filterGroup:String?=null

    override fun onCreate(s:Bundle?){
        ThemeManager.apply(this); super.onCreate(s); setContentView(R.layout.activity_tabs_overview)
        supportActionBar?.hide(); currentTabId=intent.getStringExtra("current_tab_id")?:""
        rv=findViewById(R.id.tabs_recycler); rv.layoutManager=GridLayoutManager(this,2)
        adapter=TabAdapter(currentTabId,{tab->selectTab(tab)},{tab->
            mgr.removeTab(this,tab.id)
            if(tab.id==currentTabId&&mgr.tabs.isNotEmpty())currentTabId=mgr.tabs.last().id
            refresh()
        }); rv.adapter=adapter; buildChips(); refresh()
        findViewById<FloatingActionButton>(R.id.fab_new_tab).setOnClickListener{
            setResult(RESULT_OK,Intent().putExtra("new_tab",true));finish()}
        findViewById<View>(R.id.btn_close_view).setOnClickListener{finish()}
        findViewById<View>(R.id.btn_delete_all).setOnClickListener{
            android.app.AlertDialog.Builder(this).setTitle("Close all tabs?")
                .setPositiveButton("Yes"){_,_->mgr.tabs.toList().forEach{mgr.removeTab(this,it.id)};finish()}
                .setNegativeButton("Cancel",null).show()
        }
    }
    private fun buildChips(){
        val cg=findViewById<ChipGroup>(R.id.group_chips); cg.removeAllViews()
        fun chip(label:String,gid:String?){
            cg.addView(Chip(this).apply{text=label;isCheckable=true;isChecked=(gid==filterGroup)
                setOnCheckedChangeListener{_,c->if(c){filterGroup=gid;refresh()}}})
        }
        chip("All",null); mgr.groups.forEach{chip(it.name,it.id)}
    }
    private fun refresh(){
        val list=if(filterGroup==null)mgr.tabs else mgr.tabs.filter{it.groupId==filterGroup}
        adapter.update(list); findViewById<TextView>(R.id.tv_tab_title).text="Tabs (\${mgr.tabs.size})"
    }
    private fun selectTab(tab:Tab){
        setResult(RESULT_OK,Intent().putExtra("selected_tab_id",tab.id).putExtra("selected_tab_url",tab.url)); finish()
    }
}

class TabAdapter(private var curId:String,private val onClick:(Tab)->Unit,private val onClose:(Tab)->Unit)
    :RecyclerView.Adapter<TabAdapter.VH>(){
    private var list=listOf<Tab>(); fun update(l:List<Tab>){list=l;notifyDataSetChanged()}
    inner class VH(v:View):RecyclerView.ViewHolder(v){
        val title=v.findViewById<TextView>(R.id.tab_title); val url=v.findViewById<TextView>(R.id.tab_url)
        val thumb=v.findViewById<ImageView>(R.id.tab_thumb); val close=v.findViewById<View>(R.id.btn_close_tab)
        val gBar=v.findViewById<View>(R.id.group_indicator)
    }
    override fun onCreateViewHolder(p:ViewGroup,t:Int)=VH(LayoutInflater.from(p.context).inflate(R.layout.item_tab_grid,p,false))
    override fun getItemCount()=list.size
    override fun onBindViewHolder(h:VH,pos:Int){
        val tab=list[pos]
        h.title.text=tab.title.ifEmpty{"New Tab"}; h.url.text=tab.url
        val bm=TabManager.getThumb(tab.id)
        if(bm!=null){h.thumb.setImageBitmap(bm);h.thumb.visibility=View.VISIBLE}else h.thumb.visibility=View.GONE
        val g=TabManager.groups.find{it.id==tab.groupId}
        if(g!=null){h.gBar.visibility=View.VISIBLE
            val cols=listOf(0xFFBB86FC,0xFF4A9EFF,0xFF00C853,0xFFFF6D00,0xFFF44336)
            h.gBar.setBackgroundColor(cols[g.color.coerceIn(0,4)].toInt())}
        else h.gBar.visibility=View.GONE
        h.itemView.alpha=if(tab.id==curId)1f else 0.85f
        h.itemView.setOnClickListener{onClick(tab)}; h.close.setOnClickListener{onClose(tab)}
    }
}
