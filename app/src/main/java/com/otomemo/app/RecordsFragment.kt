package com.otomemo.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class RecordsFragment : Fragment(R.layout.fragment_records) {

    private val records = mutableListOf<ServiceRecord>()
    private lateinit var adapter: RecordAdapter

    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) reload()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RecordAdapter(records) { position ->
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus catatan?")
                .setMessage("${records[position].vehicleName} - ${records[position].items}")
                .setPositiveButton("Hapus") { _, _ ->
                    records.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    Db.saveRecords(requireContext(), records)
                    updateHeader()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecordsFragment.adapter
        }
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            formLauncher.launch(Intent(requireContext(), FormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        records.clear()
        records.addAll(Db.loadRecords(requireContext()).sortedByDescending { it.id })
        adapter.notifyDataSetChanged()
        view?.findViewById<TextView>(R.id.emptyState)?.visibility =
            if (records.isEmpty()) View.VISIBLE else View.GONE
        updateHeader()
    }

    private fun updateHeader() {
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        view?.findViewById<TextView>(R.id.textTotal)?.text =
            "Total biaya servis: Rp ${nf.format(records.sumOf { it.cost })}"
    }
}
