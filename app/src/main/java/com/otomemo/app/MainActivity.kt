package com.otomemo.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val records = mutableListOf<ServiceRecord>()
    private lateinit var adapter: RecordAdapter
    private lateinit var totalText: TextView
    private lateinit var emptyState: TextView

    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            reload()
            Toast.makeText(this, "Catatan servis tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        totalText = findViewById(R.id.textTotal)
        emptyState = findViewById(R.id.emptyState)
        val recycler = findViewById<RecyclerView>(R.id.recyclerRecords)
        val fab = findViewById<FloatingActionButton>(R.id.fabAdd)

        adapter = RecordAdapter(records) { position ->
            AlertDialog.Builder(this)
                .setTitle("Hapus catatan?")
                .setMessage("${records[position].vehicleName} - ${records[position].items}")
                .setPositiveButton("Hapus") { _, _ ->
                    records.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    RecordStore.save(this, records)
                    updateTotal()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        reload()

        fab.setOnClickListener {
            formLauncher.launch(Intent(this, FormActivity::class.java))
        }
    }

    private fun reload() {
        records.clear()
        records.addAll(RecordStore.load(this).sortedByDescending { it.id })
        adapter.notifyDataSetChanged()
        emptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        updateTotal()
    }

    private fun updateTotal() {
        val total = records.sumOf { it.cost }
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        totalText.text = "Total biaya servis: Rp ${nf.format(total)}"
    }
}
