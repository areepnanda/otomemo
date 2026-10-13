package com.otomemo.app

import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class VehiclesFragment : MasterListFragment(R.layout.fragment_list) {

    private lateinit var vehicles: MutableList<Vehicle>

    override fun loadRows(): List<MasterRow> {
        vehicles = Db.loadVehicles(requireContext())
        return vehicles.map { MasterRow(it.id, it.name, it.plate) }
    }

    override fun onAdd() = showVehicleDialog(null)

    override fun onEdit(row: MasterRow) = showVehicleDialog(vehicles.first { it.id == row.id })

    override fun onDelete(row: MasterRow) {
        vehicles.removeAll { it.id == row.id }
        Db.saveVehicles(requireContext(), vehicles)
    }

    private fun showVehicleDialog(existing: Vehicle?) {
        val view = layoutInflater.inflate(R.layout.dialog_vehicle, null)
        val inputName = view.findViewById<EditText>(R.id.inputName)
        val inputPlate = view.findViewById<EditText>(R.id.inputPlate)
        existing?.let {
            inputName.setText(it.name)
            inputPlate.setText(it.plate)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Tambah Kendaraan" else "Edit Kendaraan")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val name = inputName.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                if (existing == null) {
                    vehicles.add(Vehicle(System.currentTimeMillis(), name, inputPlate.text.toString().trim()))
                } else {
                    existing.name = name
                    existing.plate = inputPlate.text.toString().trim()
                }
                Db.saveVehicles(requireContext(), vehicles)
                reload()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
