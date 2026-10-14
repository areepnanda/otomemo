package com.otomemo.app

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout

class ItemsFragment : MasterListFragment(R.layout.fragment_items) {

    private lateinit var items: MutableList<ServiceItem>
    private var selectedType = Db.TYPE_MOBIL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabs = view.findViewById<TabLayout>(R.id.tabType)
        tabs.addTab(tabs.newTab().setText(Db.typeLabel(Db.TYPE_MOBIL)))
        tabs.addTab(tabs.newTab().setText(Db.typeLabel(Db.TYPE_MOTOR)))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedType = if (tab.position == 0) Db.TYPE_MOBIL else Db.TYPE_MOTOR
                reload()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun loadRows(): List<MasterRow> {
        items = Db.loadItems(requireContext())
        return items.filter { it.type == selectedType }.map {
            MasterRow(it.id, it.name, "")
        }
    }

    override fun onAdd() = showItemDialog(null)

    override fun onEdit(row: MasterRow) = showItemDialog(items.first { it.id == row.id })

    override fun onDelete(row: MasterRow) {
        items.removeAll { it.id == row.id }
        Db.saveItems(requireContext(), items)
        SheetSync.delete(requireContext(), "items", row.id)
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
        val typeIndex = if ((existing?.type ?: selectedType) == Db.TYPE_MOBIL) 0 else 1
        spinnerType.setSelection(typeIndex)
        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Tambah Item Servis" else "Edit Item Servis")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val type = if (spinnerType.selectedItemPosition == 0) Db.TYPE_MOBIL else Db.TYPE_MOTOR
                if (existing == null) {
                    val item = ServiceItem(System.currentTimeMillis(), name, type)
                    items.add(item)
                    Db.saveItems(requireContext(), items)
                    SheetSync.upsert(requireContext(), "items", item.toJson())
                } else {
                    existing.name = name
                    existing.type = type
                    Db.saveItems(requireContext(), items)
                    SheetSync.upsert(requireContext(), "items", existing.toJson())
                }
                reload()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
