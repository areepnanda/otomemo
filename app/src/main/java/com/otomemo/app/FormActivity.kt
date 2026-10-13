package com.otomemo.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormActivity : AppCompatActivity() {

    private lateinit var vehicles: MutableList<Vehicle>
    private lateinit var workshops: MutableList<Workshop>
    private lateinit var items: MutableList<ServiceItem>
    private val checkBoxes = mutableListOf<Pair<ServiceItem, CheckBox>>()
    private var selectedVehicle: Vehicle? = null
    private var selectedWorkshop: Workshop? = null

    private fun display(v: Vehicle): String =
        listOf(v.brand, v.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "-" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        vehicles = Db.loadVehicles(this)
        workshops = Db.loadWorkshops(this)
        items = Db.loadItems(this)

        val spinnerVehicle = findViewById<Spinner>(R.id.spinnerVehicle)
        val spinnerWorkshop = findViewById<Spinner>(R.id.spinnerWorkshop)
        val inputDate = findViewById<EditText>(R.id.inputDate)
        val inputCost = findViewById<EditText>(R.id.inputCost)
        val inputNotes = findViewById<EditText>(R.id.inputNotes)
        val checkContainer = findViewById<LinearLayout>(R.id.checkContainer)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        findViewById<TextView>(R.id.textNoVehicle).visibility =
            if (vehicles.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.textNoItem).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE

        val vehicleLabels = mutableListOf("— Pilih kendaraan —")
        vehicleLabels.addAll(vehicles.map { display(it) })
        spinnerVehicle.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, vehicleLabels
        )
        spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedVehicle = if (position == 0) null else vehicles[position - 1]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedVehicle = null
            }
        }

        val workshopLabels = mutableListOf("— Tanpa bengkel —")
        workshopLabels.addAll(workshops.map { it.name })
        spinnerWorkshop.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, workshopLabels
        )
        spinnerWorkshop.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedWorkshop = if (position == 0) null else workshops[position - 1]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedWorkshop = null
            }
        }

        items.forEach { item ->
            val checkBox = CheckBox(this)
            checkBox.text = item.name
            checkContainer.addView(checkBox)
            checkBoxes.add(item to checkBox)
        }

        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        inputDate.setText(dateFormat.format(cal.time))
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

        buttonSave.setOnClickListener {
            val vehicle = selectedVehicle
            if (vehicle == null) {
                Toast.makeText(this, "Pilih kendaraan dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val chosenItems = checkBoxes.filter { it.second.isChecked }.map { it.first.name }
            if (chosenItems.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 item service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val record = ServiceRecord(
                id = System.currentTimeMillis(),
                vehicleName = display(vehicle),
                plateNumber = vehicle.plate,
                date = inputDate.text.toString().trim(),
                workshop = selectedWorkshop?.name ?: "",
                items = chosenItems.joinToString(", "),
                cost = inputCost.text.toString().trim().toLongOrNull() ?: 0L,
                notes = inputNotes.text.toString().trim()
            )

            val records = Db.loadRecords(this)
            records.add(record)
            Db.saveRecords(this, records)

            setResult(RESULT_OK)
            finish()
        }
    }
}
