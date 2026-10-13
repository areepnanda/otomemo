package com.otomemo.app

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class MasterListFragment(layoutRes: Int) : Fragment(layoutRes) {

    private val rows = mutableListOf<MasterRow>()
    private lateinit var adapter: MasterAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MasterAdapter(
            rows,
            onClick = { onEdit(rows[it]) },
            onLongClick = { confirmDelete(rows[it]) }
        )
        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MasterListFragment.adapter
        }
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { onAdd() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        rows.clear()
        rows.addAll(loadRows())
        adapter.notifyDataSetChanged()
        view?.findViewById<TextView>(R.id.emptyState)?.visibility =
            if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(row: MasterRow) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus?")
            .setMessage(row.title)
            .setPositiveButton("Hapus") { _, _ ->
                deleteRow(row)
                reload()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    protected fun textDialog(title: String, current: String?, onSave: (String) -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_text, null)
        val input = view.findViewById<EditText>(R.id.inputText)
        input.setText(current)
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    onSave(value)
                    reload()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    protected abstract fun loadRows(): List<MasterRow>
    protected abstract fun onAdd()
    protected abstract fun onEdit(row: MasterRow)
    protected abstract fun onDelete(row: MasterRow)
}
