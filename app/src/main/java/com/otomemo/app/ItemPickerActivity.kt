package com.otomemo.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ItemPickerActivity : AppCompatActivity() {

    private lateinit var items: MutableList<ServiceItem>
    private val selected = LinkedHashSet<String>()
    private lateinit var adapter: PickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_picker)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: Db.TYPE_MOTOR
        selected.addAll(intent.getStringArrayExtra(EXTRA_SELECTED)?.toList() ?: emptyList())

        findViewById<TextView>(R.id.textPickerType).text =
            "Kendaraan: ${Db.typeLabel(type)}"

        adapter = PickerAdapter()
        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@ItemPickerActivity)
            adapter = this@ItemPickerActivity.adapter
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddDialog(type)
        }

        findViewById<Button>(R.id.buttonOk).setOnClickListener {
            val result = items.filter { it.name in selected }.map { it.name }
            val data = Intent().putExtra(EXTRA_RESULT, result.toTypedArray())
            setResult(RESULT_OK, data)
            finish()
        }

        reload(type)
    }

    private fun reload(type: String) {
        items = Db.loadItems(this).filter { it.type == type }.toMutableList()
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.emptyState).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog(type: String) {
        val view = layoutInflater.inflate(R.layout.dialog_text, null)
        val input = view.findViewById<EditText>(R.id.inputText)
        AlertDialog.Builder(this)
            .setTitle("Tambah Item Servis (${Db.typeLabel(type)})")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val allItems = Db.loadItems(this)
                val item = ServiceItem(System.currentTimeMillis(), name, type)
                allItems.add(item)
                Db.saveItems(this, allItems)
                SheetSync.upsert(this, "items", item.toJson())
                selected.add(name)
                reload(type)
                Toast.makeText(this, "Item ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private inner class PickerAdapter : RecyclerView.Adapter<PickerAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.checkItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_picker, parent, false)
            return Holder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.text = item.name
            holder.checkBox.isChecked = item.name in selected
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(item.name) else selected.remove(item.name)
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_SELECTED = "extra_selected"
        const val EXTRA_RESULT = "extra_result"
    }
}
