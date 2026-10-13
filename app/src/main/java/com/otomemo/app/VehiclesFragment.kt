package com.otomemo.app

import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class VehiclesFragment : MasterListFragment(R.layout.fragment_list) {

    private lateinit var vehicles: MutableList<Vehicle>

    override fun loadRows(): List<MasterRow> {
        vehicles = Db.loadVehicles(requireContext())
        return vehicles.map {
            MasterRow(it.id, display(it), subtitle(it))
        }
    }

    override fun onAdd() = showVehicleDialog(null)

    override fun onEdit(row: MasterRow) = showVehicleDialog(vehicles.first { it.id == row.id })

    override fun onDelete(row: MasterRow) {
        vehicles.removeAll { it.id == row.id }
        Db.saveVehicles(requireContext(), vehicles)
    }

    private fun display(v: Vehicle): String =
        listOf(v.brand, v.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "-" }

    private fun subtitle(v: Vehicle): String =
        listOf(v.year, v.plate).filter { it.isNotEmpty() }.joinToString("  •  ")

    private fun showVehicleDialog(existing: Vehicle?) {
        val view = layoutInflater.inflate(R.layout.dialog_vehicle, null)
        val inputBrand = view.findViewById<EditText>(R.id.inputBrand)
        val inputModel = view.findViewById<EditText>(R.id.inputModel)
        val inputYear = view.findViewById<EditText>(R.id.inputYear)
        val inputPlate = view.findViewById<EditText>(R.id.inputPlate)
        existing?.let {
            inputBrand.setText(it.brand)
            inputModel.setText(it.model)
            inputYear.setText(it.year)
            inputPlate.setText(it.plate)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Tambah Kendaraan" else "Edit Kendaraan")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val brand = inputBrand.text.toString().trim()
                if (brand.isEmpty()) return@setPositiveButton
                if (existing == null) {
                    vehicles.add(
                        Vehicle(
                            System.currentTimeMillis(),
                            brand,
                            inputModel.text.toString().trim(),
                            inputYear.text.toString().trim(),
                            inputPlate.text.toString().trim()
                        )
                    )
                } else {
                    existing.brand = brand
                    existing.model = inputModel.text.toString().trim()
                    existing.year = inputYear.text.toString().trim()
                    existing.plate = inputPlate.text.toString().trim()
                }
                Db.saveVehicles(requireContext(), vehicles)
                reload()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
