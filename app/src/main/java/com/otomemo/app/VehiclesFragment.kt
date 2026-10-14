package com.otomemo.app

import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
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
        SheetSync.delete(requireContext(), "vehicles", row.id)
    }

    private fun display(v: Vehicle): String =
        listOf(v.brand, v.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "-" }

    private fun subtitle(v: Vehicle): String {
        val parts = mutableListOf(Db.typeLabel(v.type))
        parts.addAll(listOf(v.year, v.plate).filter { it.isNotEmpty() })
        return parts.joinToString("  •  ")
    }

    private fun showVehicleDialog(existing: Vehicle?) {
        val view = layoutInflater.inflate(R.layout.dialog_vehicle, null)
        val inputBrand = view.findViewById<EditText>(R.id.inputBrand)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerType)
        val inputModel = view.findViewById<EditText>(R.id.inputModel)
        val inputYear = view.findViewById<EditText>(R.id.inputYear)
        val inputPlate = view.findViewById<EditText>(R.id.inputPlate)
        spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(Db.typeLabel(Db.TYPE_MOBIL), Db.typeLabel(Db.TYPE_MOTOR))
        )
        existing?.let {
            inputBrand.setText(it.brand)
            spinnerType.setSelection(if (it.type == Db.TYPE_MOBIL) 0 else 1)
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
                val type = if (spinnerType.selectedItemPosition == 0) Db.TYPE_MOBIL else Db.TYPE_MOTOR
                if (existing == null) {
                    val vehicle = Vehicle(
                        System.currentTimeMillis(),
                        brand,
                        inputModel.text.toString().trim(),
                        inputYear.text.toString().trim(),
                        inputPlate.text.toString().trim(),
                        type
                    )
                    vehicles.add(vehicle)
                    Db.saveVehicles(requireContext(), vehicles)
                    SheetSync.upsert(requireContext(), "vehicles", vehicle.toJson())
                } else {
                    existing.brand = brand
                    existing.type = type
                    existing.model = inputModel.text.toString().trim()
                    existing.year = inputYear.text.toString().trim()
                    existing.plate = inputPlate.text.toString().trim()
                    Db.saveVehicles(requireContext(), vehicles)
                    SheetSync.upsert(requireContext(), "vehicles", existing.toJson())
                }
                reload()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
