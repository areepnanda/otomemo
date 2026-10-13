package com.otomemo.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        val inputVehicle = findViewById<EditText>(R.id.inputVehicle)
        val inputPlate = findViewById<EditText>(R.id.inputPlate)
        val inputDate = findViewById<EditText>(R.id.inputDate)
        val inputWorkshop = findViewById<EditText>(R.id.inputWorkshop)
        val inputItems = findViewById<EditText>(R.id.inputItems)
        val inputCost = findViewById<EditText>(R.id.inputCost)
        val inputNotes = findViewById<EditText>(R.id.inputNotes)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
            val vehicle = inputVehicle.text.toString().trim()
            val items = inputItems.text.toString().trim()
            if (vehicle.isEmpty() || items.isEmpty()) {
                Toast.makeText(this, "Nama kendaraan dan item servis wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val record = ServiceRecord(
                id = System.currentTimeMillis(),
                vehicleName = vehicle,
                plateNumber = inputPlate.text.toString().trim(),
                date = if (inputDate.text.isNullOrBlank()) dateFormat.format(cal.time)
                else inputDate.text.toString().trim(),
                workshop = inputWorkshop.text.toString().trim(),
                items = items,
                cost = inputCost.text.toString().trim().toLongOrNull() ?: 0L,
                notes = inputNotes.text.toString().trim()
            )

            val records = RecordStore.load(this)
            records.add(record)
            RecordStore.save(this, records)

            setResult(RESULT_OK)
            finish()
        }
    }
}
