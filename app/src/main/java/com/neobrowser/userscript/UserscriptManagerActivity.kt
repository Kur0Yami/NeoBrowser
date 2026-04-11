package com.neobrowser.userscript

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.neobrowser.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class UserscriptManagerActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScriptAdapter
    private lateinit var manager: UserscriptManager

    // Launcher untuk Storage Access Framework (Pilih File)
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { readScriptFromUri(it) }
    }

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
        fab.setOnClickListener {
            // Ubah FAB jadi menu pilihan
            val options = arrayOf("Tulis Manual", "Import dari URL", "Import dari File Manager")
            AlertDialog.Builder(this)
                .setTitle("Tambah Userscript")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showEditDialog(null)
                        1 -> showImportUrlDialog()
                        2 -> filePickerLauncher.launch("*/*")
                    }
                }
                .show()
        }
    }

    private fun refreshList() {
        adapter.setScripts(manager.getAllScripts())
    }

    // [FUNGSI ASLI LU TETAP SAMA]
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

    // --- FITUR BARU: IMPORT URL ---
    private fun showImportUrlDialog() {
        val input = EditText(this).apply {
            hint = "https://example.com/script.user.js"
            setPadding(50, 50, 50, 50)
        }
        AlertDialog.Builder(this)
            .setTitle("Import dari URL")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val urlStr = input.text.toString()
                if (urlStr.isNotBlank()) fetchScriptFromUrl(urlStr)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun fetchScriptFromUrl(urlString: String) {
        Toast.makeText(this, "Mengunduh...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val scriptContent = connection.inputStream.bufferedReader().use { it.readText() }

                withContext(Dispatchers.Main) {
                    if (scriptContent.contains("==UserScript==")) {
                        parseAndSaveUserscript(scriptContent)
                        Toast.makeText(this@UserscriptManagerActivity, "Script berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@UserscriptManagerActivity, "URL tidak berisi userscript yang valid", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserscriptManagerActivity, "Gagal mengunduh script.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- FITUR BARU: IMPORT FILE MANAGER ---
    private fun readScriptFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val scriptContent = inputStream?.bufferedReader()?.use { it.readText() }

            if (!scriptContent.isNullOrEmpty() && scriptContent.contains("==UserScript==")) {
                parseAndSaveUserscript(scriptContent)
                Toast.makeText(this, "Script berhasil ditambahkan dari file!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Format file tidak valid (bukan userscript)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membaca file", Toast.LENGTH_SHORT).show()
        }
    }

    // --- FITUR BARU: PARSER METADATA ---
    private fun parseAndSaveUserscript(code: String) {
        var scriptName = "Imported Script"
        var scriptDesc = ""
        val matchPattern = mutableListOf<String>()
        var runAt = "document-end"

        // Ekstrak metadata blok Greasemonkey/Tampermonkey
        val lines = code.split("\n")
        var inMetadata = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "// ==UserScript==") inMetadata = true
            else if (trimmed == "// ==/UserScript==") break
            else if (inMetadata) {
                if (trimmed.startsWith("// @name ")) scriptName = trimmed.substringAfter("// @name").trim()
                else if (trimmed.startsWith("// @description ")) scriptDesc = trimmed.substringAfter("// @description").trim()
                else if (trimmed.startsWith("// @match ")) matchPattern.add(trimmed.substringAfter("// @match").trim())
                else if (trimmed.startsWith("// @run-at ")) runAt = trimmed.substringAfter("// @run-at").trim()
            }
        }
        
        // Fallback kalau gak ada tag @match
        if (matchPattern.isEmpty()) matchPattern.add("*://*/*")

        manager.addScript(Userscript(
            name = scriptName,
            description = scriptDesc,
            matches = matchPattern,
            code = code,
            runAt = runAt
        ))
        refreshList()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// [ADAPTER ASLI LU TETAP SAMA]
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
