package com.otomemo.app

import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog

class ItemsFragment : MasterListFragment(R.layout.fragment_list) {

    private lateinit var items: MutableList<ServiceItem>

    override fun loadRows(): List<MasterRow> {
        items = Db.loadItems(requireContext())
        return items.map {
            MasterRow(it.id, it.name, Db.typeLabel(it.type))
        }
    }

    override fun onAdd() = showItemDialog(null)

    override fun onEdit(row: MasterRow) = showItemDialog(items.first { it.id == row.id })

    override fun onDelete(row: MasterRow) {
        items.removeAll { it.id == row.id }
        Db.saveItems(requireContext(), items)
    }

    private fun showItemDialog(existing: ServiceItem?) {
        val view = layoutInflater.inflate(R.layout.dialog_item, null)
        val input = view.findViewById<EditText>(R.id.inputText)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerType)
        spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(Db.typeLabel(Db.TYPE_MOBIL), Db.typeLabel(Db.TYPE_MOTOR))
        )
        input.setText(existing?.name)
        existing?.let {
            spinnerType.setSelection(if (it.type == Db.TYPE_MOBIL) 0 else 1)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Tambah Item Servis" else "Edit Item Servis")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val type = if (spinnerType.selectedItemPosition == 0) Db.TYPE_MOBIL else Db.TYPE_MOTOR
                if (existing == null) {
                    items.add(ServiceItem(System.currentTimeMillis(), name, type))
                } else {
                    existing.name = name
                    existing.type = type
                }
                Db.saveItems(requireContext(), items)
                reload()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
