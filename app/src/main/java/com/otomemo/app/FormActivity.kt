package com.otomemo.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormActivity : AppCompatActivity() {

    private lateinit var vehicles: MutableList<Vehicle>
    private lateinit var workshops: MutableList<Workshop>
    private lateinit var spinnerVehicles: MutableList<Vehicle>
    private lateinit var spinnerWorkshops: MutableList<Workshop>
    private val selectedItemNames = LinkedHashSet<String>()
    private var selectedVehicle: Vehicle? = null
    private var selectedWorkshop: Workshop? = null
    private var currentVehicleId = -1L
    private var editRecord: ServiceRecord? = null

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val names = result.data?.getStringArrayExtra(ItemPickerActivity.EXTRA_RESULT)?.toList()
            selectedItemNames.clear()
            names?.let { selectedItemNames.addAll(it) }
            updateItemsInput()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        vehicles = Db.loadVehicles(this)
        workshops = Db.loadWorkshops(this)

        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (recordId >= 0) {
            editRecord = Db.loadRecords(this).firstOrNull { it.id == recordId }
        }
        val editing = editRecord

        val spinnerVehicle = findViewById<Spinner>(R.id.spinnerVehicle)
        val spinnerWorkshop = findViewById<Spinner>(R.id.spinnerWorkshop)
        val inputDate = findViewById<EditText>(R.id.inputDate)
        val inputOdometer = findViewById<EditText>(R.id.inputOdometer)
        val inputItems = findViewById<EditText>(R.id.inputItems)
        val inputCost = findViewById<EditText>(R.id.inputCost)
        val inputNotes = findViewById<EditText>(R.id.inputNotes)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        findViewById<TextView>(R.id.textFormTitle).text =
            if (editing == null) "Tambah Catatan Service" else "Edit Catatan Service"

        findViewById<TextView>(R.id.textNoVehicle).visibility =
            if (vehicles.isEmpty() && editing == null) View.VISIBLE else View.GONE

        // Kendaraan (master + snapshot record yang sedang diedit)
        spinnerVehicles = vehicles.toMutableList()
        var vehicleIndex = -1
        if (editing != null) {
            vehicleIndex = spinnerVehicles.indexOfFirst {
                it.id == editing.vehicleId ||
                    (it.plate == editing.plateNumber && it.plate.isNotEmpty()) ||
                    display(it) == editing.vehicleName
            }
            if (vehicleIndex < 0) {
                spinnerVehicles.add(
                    Vehicle(editing.vehicleId, editing.vehicleName, "", "", editing.plateNumber)
                )
                vehicleIndex = spinnerVehicles.lastIndex
            }
        }

        val vehicleLabels = mutableListOf("— Pilih kendaraan —")
        vehicleLabels.addAll(spinnerVehicles.map { display(it) })
        spinnerVehicle.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, vehicleLabels
        )
        spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedVehicle = if (position == 0) null else spinnerVehicles[position - 1]
                val newId = selectedVehicle?.id ?: -1L
                if (newId != currentVehicleId) {
                    selectedItemNames.clear()
                    currentVehicleId = newId
                    updateItemsInput()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedVehicle = null
            }
        }

        // Bengkel (master + snapshot record yang sedang diedit)
        spinnerWorkshops = workshops.toMutableList()
        var workshopIndex = -1
        if (editing != null && editing.workshop.isNotEmpty()) {
            workshopIndex = spinnerWorkshops.indexOfFirst { it.name == editing.workshop }
            if (workshopIndex < 0) {
                spinnerWorkshops.add(Workshop(-1L, editing.workshop))
                workshopIndex = spinnerWorkshops.lastIndex
            }
        }

        val workshopLabels = mutableListOf("— Tanpa bengkel —")
        workshopLabels.addAll(spinnerWorkshops.map { it.name })
        spinnerWorkshop.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, workshopLabels
        )
        spinnerWorkshop.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedWorkshop = if (position == 0) null else spinnerWorkshops[position - 1]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedWorkshop = null
            }
        }

        // Prefill saat edit
        if (editing != null) {
            selectedVehicle = spinnerVehicles[vehicleIndex]
            currentVehicleId = selectedVehicle?.id ?: -1L
            spinnerVehicle.setSelection(vehicleIndex + 1)
            if (workshopIndex >= 0) {
                selectedWorkshop = spinnerWorkshops[workshopIndex]
                spinnerWorkshop.setSelection(workshopIndex + 1)
            }
            selectedItemNames.addAll(
                editing.items.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            )
        }

        // Input item service (klik untuk buka laman pilih item)
        inputItems.setOnClickListener {
            val vehicle = selectedVehicle
            if (vehicle == null) {
                Toast.makeText(this, "Pilih kendaraan dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ItemPickerActivity::class.java)
                .putExtra(ItemPickerActivity.EXTRA_TYPE, vehicle.type)
                .putExtra(ItemPickerActivity.EXTRA_SELECTED, selectedItemNames.toTypedArray())
            pickerLauncher.launch(intent)
        }

        // Tanggal
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        if (editing != null) {
            inputDate.setText(editing.date)
        } else {
            inputDate.setText(dateFormat.format(cal.time))
        }
        inputDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    inputDate.setText(dateFormat.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Odometer, biaya, catatan
        if (editing != null) {
            if (editing.odometer > 0) inputOdometer.setText(editing.odometer.toString())
            if (editing.cost > 0) {
                inputCost.setText(NumberFormat.getInstance(Locale("in", "ID")).format(editing.cost))
            }
            inputNotes.setText(editing.notes)
        }

        // Biaya auto-format ribuan
        val numberFormat = NumberFormat.getInstance(Locale("in", "ID"))
        var formattingCost = false
        inputCost.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (formattingCost || s == null) return
                formattingCost = true
                val digits = s.toString().filter { it.isDigit() }.take(15)
                if (digits.isEmpty()) {
                    s.clear()
                } else {
                    val formatted = numberFormat.format(digits.toLong())
                    if (s.toString() != formatted) s.replace(0, s.length, formatted)
                }
                formattingCost = false
            }
        })

        buttonSave.setOnClickListener {
            val vehicle = selectedVehicle
            if (vehicle == null) {
                Toast.makeText(this, "Pilih kendaraan dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedItemNames.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 item service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val record = ServiceRecord(
                id = editing?.id ?: System.currentTimeMillis(),
                vehicleId = vehicle.id,
                vehicleName = display(vehicle),
                plateNumber = vehicle.plate,
                date = inputDate.text.toString().trim(),
                workshop = selectedWorkshop?.name ?: "",
                odometer = inputOdometer.text.toString().filter { it.isDigit() }.toLongOrNull() ?: 0L,
                items = selectedItemNames.joinToString(", "),
                cost = inputCost.text.toString().filter { it.isDigit() }.toLongOrNull() ?: 0L,
                notes = inputNotes.text.toString().trim()
            )

            val records = Db.loadRecords(this)
            val existingIndex = records.indexOfFirst { it.id == record.id }
            if (existingIndex >= 0) records[existingIndex] = record else records.add(record)
            Db.saveRecords(this, records)

            setResult(RESULT_OK)
            finish()
        }
    }

    private fun updateItemsInput() {
        findViewById<EditText>(R.id.inputItems).setText(selectedItemNames.joinToString(", "))
    }

    private fun display(v: Vehicle): String =
        listOf(v.brand, v.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "-" }

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }
}
