package com.neobrowser.userscript

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.neobrowser.R

class UserscriptManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScriptAdapter
    private lateinit var manager: UserscriptManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userscript_manager)

        manager = UserscriptManager.getInstance(this)

        supportActionBar?.title = "Userscript Manager"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.scripts_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ScriptAdapter(
            onToggle = { script ->
                manager.toggleScript(script.id)
                refreshList()
            },
            onEdit = { script -> showEditDialog(script) },
            onDelete = { script ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Script")
                    .setMessage("Delete '${script.name}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        manager.deleteScript(script.id)
                        refreshList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.adapter = adapter
        refreshList()

        val fab = findViewById<FloatingActionButton>(R.id.fab_add_script)
        fab.setOnClickListener { showEditDialog(null) }
    }

    private fun refreshList() {
        adapter.setScripts(manager.getAllScripts())
    }

    private fun showEditDialog(existingScript: Userscript?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_script, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.input_name)
        val descInput = dialogView.findViewById<EditText>(R.id.input_description)
        val matchesInput = dialogView.findViewById<EditText>(R.id.input_matches)
        val codeInput = dialogView.findViewById<EditText>(R.id.input_code)
        val runAtSpinner = dialogView.findViewById<Spinner>(R.id.spinner_run_at)

        val runAtOptions = arrayOf("document-end", "document-start", "document-idle")
        runAtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, runAtOptions)

        existingScript?.let {
            nameInput.setText(it.name)
            descInput.setText(it.description)
            matchesInput.setText(it.matches.joinToString(", "))
            codeInput.setText(it.code)
            runAtSpinner.setSelection(runAtOptions.indexOf(it.runAt))
        }

        AlertDialog.Builder(this)
            .setTitle(if (existingScript == null) "New Userscript" else "Edit Userscript")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().ifBlank { "Unnamed Script" }
                val matches = matchesInput.text.toString()
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .ifEmpty { listOf("*") }

                if (existingScript == null) {
                    manager.addScript(Userscript(
                        name = name,
                        description = descInput.text.toString(),
                        matches = matches,
                        code = codeInput.text.toString(),
                        runAt = runAtOptions[runAtSpinner.selectedItemPosition]
                    ))
                } else {
                    manager.updateScript(existingScript.copy(
                        name = name,
                        description = descInput.text.toString(),
                        matches = matches,
                        code = codeInput.text.toString(),
                        runAt = runAtOptions[runAtSpinner.selectedItemPosition]
                    ))
                }
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class ScriptAdapter(
    private val onToggle: (Userscript) -> Unit,
    private val onEdit: (Userscript) -> Unit,
    private val onDelete: (Userscript) -> Unit
) : RecyclerView.Adapter<ScriptAdapter.ScriptViewHolder>() {

    private var scripts = listOf<Userscript>()

    fun setScripts(list: List<Userscript>) {
        scripts = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_userscript, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        holder.bind(scripts[position])
    }

    override fun getItemCount() = scripts.size

    inner class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.script_name)
        private val desc: TextView = itemView.findViewById(R.id.script_desc)
        private val matches: TextView = itemView.findViewById(R.id.script_matches)
        private val toggle: Switch = itemView.findViewById(R.id.script_toggle)
        private val editBtn: ImageButton = itemView.findViewById(R.id.btn_edit_script)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.btn_delete_script)

        fun bind(script: Userscript) {
            name.text = script.name
            desc.text = script.description.ifBlank { "No description" }
            matches.text = "Matches: ${script.matches.joinToString(", ")}"
            toggle.isChecked = script.enabled
            toggle.setOnCheckedChangeListener(null)
            toggle.setOnCheckedChangeListener { _, _ -> onToggle(script) }
            editBtn.setOnClickListener { onEdit(script) }
            deleteBtn.setOnClickListener { onDelete(script) }
        }
    }
}
