package com.neobrowser.tabs

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neobrowser.R

class TabsOverviewActivity : AppCompatActivity() {

    private val mgr = TabManager.instance
    private lateinit var rv: RecyclerView
    private lateinit var adapter: TabAdapter
    private lateinit var headerNormal: LinearLayout
    private lateinit var headerSelect: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSelectCount: TextView
    private lateinit var groupChipsContainer: LinearLayout
    private val selected = mutableSetOf<String>()
    private var selecting = false
    private var activeGroupId: String? = null  // null = semua tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs_overview)
        supportActionBar?.hide()

        headerNormal        = findViewById(R.id.header_normal)
        headerSelect        = findViewById(R.id.header_select)
        tvTitle             = findViewById(R.id.tv_tab_title)
        tvSelectCount       = findViewById(R.id.tv_select_count)
        groupChipsContainer = findViewById(R.id.group_chips)

        rv = findViewById(R.id.tabs_recycler)
        rv.layoutManager = GridLayoutManager(this, 2)

        adapter = TabAdapter(
            mgr            = mgr,
            getSelected    = { selected },
            isSelecting    = { selecting },
            getActiveGroup = { activeGroupId },
            onClick        = { tab ->
                if (selecting) toggleTab(tab.id)
                else { setResult(RESULT_OK, Intent().putExtra("selected_tab_id", tab.id)); finish() }
            },
            onLongClick    = { tab ->
                if (!selecting) { selecting = true; showSelectHeader() }
                toggleTab(tab.id)
            },
            onClose        = { tab ->
                mgr.closeTabAndSave(applicationContext, tab.id)
                adapter.notifyDataSetChanged()
                buildGroupChips()
                updateTitle()
            }
        )
        rv.adapter = adapter
        buildGroupChips()
        updateTitle()

        // Normal header
        findViewById<ImageButton>(R.id.btn_select_tabs).setOnClickListener {
            selecting = true
            selected.addAll(getFilteredTabs().map { it.id })
            showSelectHeader(); adapter.notifyDataSetChanged(); updateTitle()
        }
        findViewById<ImageButton>(R.id.btn_delete_all).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Delete All?")
                .setPositiveButton("Delete") { _, _ ->
                    getFilteredTabs().toList().forEach { mgr.closeTabAndSave(applicationContext, it.id) }
                    activeGroupId = null
                    adapter.notifyDataSetChanged(); buildGroupChips(); updateTitle()
                }.setNegativeButton("Cancel", null).show()
        }
        findViewById<ImageButton>(R.id.btn_normal_menu).setOnClickListener { v ->
            PopupMenu(this, v).also { p ->
                p.menu.add(0, 99, 0, "Recover closed tab")
                p.setOnMenuItemClickListener { Toast.makeText(this, "No closed tabs", Toast.LENGTH_SHORT).show(); true }
                p.show()
            }
        }
        findViewById<View>(R.id.btn_close_view).setOnClickListener { finish() }

        // Select header
        findViewById<ImageButton>(R.id.btn_back_select).setOnClickListener { clearSelect() }
        findViewById<ImageButton>(R.id.btn_select_menu).setOnClickListener { v ->
            if (selected.isEmpty()) { Toast.makeText(this, "Select tabs first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PopupMenu(this, v).also { p ->
                p.menu.add(0, 1, 0, "Group")
                p.menu.add(0, 2, 1, "Pin")
                p.menu.add(0, 3, 2, "Bookmark")
                p.menu.add(0, 4, 3, "Share")
                p.menu.add(0, 5, 4, "Delete")
                p.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> showGroupDialog()
                        2 -> { Toast.makeText(this, "Pinned", Toast.LENGTH_SHORT).show(); clearSelect() }
                        3 -> { Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show(); clearSelect() }
                        4 -> shareSelected()
                        5 -> deleteSelected()
                    }; true
                }; p.show()
            }
        }
        findViewById<View>(R.id.fab_new_tab).setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra("action", "new_tab")); finish()
        }
    }

    // Build chips: "All" + satu chip per group yang punya tab
    private fun buildGroupChips() {
        groupChipsContainer.removeAllViews()
        addChip("All", null, activeGroupId == null, 0xFFBB86FC.toInt())
        mgr.groups.forEach { group ->
            val count = mgr.tabs.count { it.groupId == group.id }
            if (count > 0) addChip("${group.name} ($count)", group.id, activeGroupId == group.id, group.color)
        }
    }

    private fun addChip(label: String, groupId: String?, isActive: Boolean, color: Int) {
        val tv = TextView(this)
        tv.text = label
        tv.textSize = 12f
        tv.setPadding(28, 10, 28, 10)
        tv.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        tv.setTextColor(if (isActive) Color.WHITE else 0xFFAAAAAA.toInt())

        val bg = GradientDrawable()
        bg.cornerRadius = 32f
        bg.setColor(if (isActive) color else 0xFF2D2D2D.toInt())
        tv.background = bg

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 8
        tv.layoutParams = lp

        tv.setOnClickListener {
            activeGroupId = groupId
            buildGroupChips()
            adapter.notifyDataSetChanged()
            updateTitle()
        }
        groupChipsContainer.addView(tv)
    }

    private fun getFilteredTabs(): List<Tab> =
        if (activeGroupId == null) mgr.tabs
        else mgr.tabs.filter { it.groupId == activeGroupId }

    private fun toggleTab(id: String) {
        if (selected.contains(id)) selected.remove(id) else selected.add(id)
        if (selected.isEmpty()) { clearSelect(); return }
        adapter.notifyDataSetChanged(); updateTitle()
    }

    private fun clearSelect() {
        selecting = false; selected.clear()
        showNormalHeader(); adapter.notifyDataSetChanged(); updateTitle()
    }

    private fun showNormalHeader() { headerNormal.visibility = View.VISIBLE; headerSelect.visibility = View.GONE }
    private fun showSelectHeader() { headerNormal.visibility = View.GONE;    headerSelect.visibility = View.VISIBLE }

    private fun updateTitle() {
        val filtered = getFilteredTabs()
        tvTitle.text = if (activeGroupId == null) "Tabs (${mgr.tabs.size})"
                       else "${mgr.groups.find { it.id == activeGroupId }?.name} (${filtered.size})"
        tvSelectCount.text = "${selected.size} Selected"
    }

    private fun showGroupDialog() {
        val tabsToGroup = selected.toList()
        if (tabsToGroup.isEmpty()) return

        val colorNames = arrayOf("Purple", "Blue", "Green", "Orange", "Red")
        val colorVals  = intArrayOf(
            Color.parseColor("#BB86FC"), Color.parseColor("#4A9EFF"),
            Color.parseColor("#00C853"), Color.parseColor("#FF6D00"),
            Color.parseColor("#F44336")
        )
        var pickedIdx = 0

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(60, 24, 60, 8)
        }
        val nameInput = EditText(this).apply { hint = "Group name" }
        val spinner   = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colorNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) { pickedIdx = pos }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        container.addView(nameInput); container.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("Group ${tabsToGroup.size} tab(s)")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val g = mgr.createGroup(nameInput.text.toString().ifBlank { "Group" }, colorVals[pickedIdx])
                tabsToGroup.forEach { mgr.assignTabToGroup(it, g.id) }
                mgr.saveTabs(applicationContext)
                selecting = false; selected.clear()
                showNormalHeader(); buildGroupChips()
                updateTitle(); adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun shareSelected() {
        val urls = selected.toList().mapNotNull { mgr.getTab(it)?.url }.joinToString("\n")
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, urls) }, "Share"))
        clearSelect()
    }

    private fun deleteSelected() {
        selected.toList().forEach { mgr.closeTabAndSave(applicationContext, it) }
        clearSelect(); adapter.notifyDataSetChanged(); buildGroupChips()
    }

    override fun onBackPressed() {
        if (selecting) clearSelect() else super.onBackPressed()
    }
}

class TabAdapter(
    private val mgr: TabManager,
    private val getSelected: () -> Set<String>,
    private val isSelecting: () -> Boolean,
    private val getActiveGroup: () -> String?,
    private val onClick: (Tab) -> Unit,
    private val onLongClick: (Tab) -> Unit,
    private val onClose: (Tab) -> Unit
) : RecyclerView.Adapter<TabAdapter.VH>() {

    private fun filtered(): List<Tab> {
        val g = getActiveGroup()
        return if (g == null) mgr.tabs else mgr.tabs.filter { it.groupId == g }
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_tab_grid, p, false))

    override fun getItemCount() = filtered().size
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(filtered()[i])

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView    = v.findViewById(R.id.tab_title)
        val url: TextView      = v.findViewById(R.id.tab_url)
        val thumb: ImageView   = v.findViewById(R.id.tab_thumb)
        val placeholder: View  = v.findViewById(R.id.thumb_placeholder)
        val close: View        = v.findViewById(R.id.btn_close_tab)
        val groupBar: View     = v.findViewById(R.id.group_indicator)
        val overlay: View      = v.findViewById(R.id.selected_overlay)
        val check: TextView    = v.findViewById(R.id.check_icon)

        fun bind(tab: Tab) {
            title.text = tab.title.take(30)
            url.text   = tab.url.replace("https://","").replace("http://","").take(40)

            val bmp = mgr.getThumb(tab.id)
            thumb.visibility       = if (bmp != null) View.VISIBLE else View.GONE
            placeholder.visibility = if (bmp != null) View.GONE    else View.VISIBLE
            if (bmp != null) thumb.setImageBitmap(bmp)

            val grp = tab.groupId?.let { gid -> mgr.groups.find { it.id == gid } }
            groupBar.visibility = if (grp != null) View.VISIBLE else View.GONE
            if (grp != null) groupBar.setBackgroundColor(grp.color)

            val sel = getSelected().contains(tab.id)
            overlay.visibility = if (sel) View.VISIBLE else View.GONE
            check.visibility   = if (sel) View.VISIBLE else View.GONE
            close.visibility   = if (isSelecting()) View.GONE else View.VISIBLE

            itemView.setOnClickListener     { onClick(tab) }
            itemView.setOnLongClickListener { onLongClick(tab); true }
            close.setOnClickListener        { onClose(tab) }
        }
    }
}
