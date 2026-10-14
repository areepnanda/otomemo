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
import java.text.SimpleDateFormat
import java.util.Locale

class RecordsFragment : Fragment(R.layout.fragment_records) {

    private val records = mutableListOf<ServiceRecord>()
    private lateinit var adapter: RecordAdapter

    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            reload()
            Toast.makeText(requireContext(), "Catatan service tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RecordAdapter(
            records,
            onClick = { showDetail(it) },
            onLongClick = { confirmDelete(it) }
        )
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

    private fun showDetail(position: Int) {
        val item = records[position]
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        val message = buildString {
            if (item.plateNumber.isNotEmpty()) append("Plat: ${item.plateNumber}\n")
            append("Tanggal: ${item.date}\n")
            if (item.odometer > 0) append("Odometer: ${nf.format(item.odometer)} km\n")
            if (item.workshop.isNotEmpty()) append("Bengkel: ${item.workshop}\n")
            append("Item: ${item.items}\n")
            if (item.images.isNotEmpty()) append("Foto: ${item.images.size}\n")
            append("Biaya: Rp ${nf.format(item.cost)}\n")
            if (item.notes.isNotEmpty()) append("Catatan: ${item.notes}")
        }
        val hasPhotos = item.images.isNotEmpty()
        AlertDialog.Builder(requireContext())
            .setTitle(item.vehicleName)
            .setMessage(message)
            .setPositiveButton("Edit") { _, _ -> openEdit(item.id) }
            .setNeutralButton(if (hasPhotos) "Foto (${item.images.size})" else "Tutup") { _, _ ->
                if (hasPhotos) {
                    startActivity(
                        Intent(requireContext(), ImagePagerActivity::class.java)
                            .putExtra(ImagePagerActivity.EXTRA_IMAGES, item.images.toTypedArray())
                    )
                }
            }
            .setNegativeButton("Hapus") { _, _ -> confirmDelete(position) }
            .show()
    }

    private fun openEdit(recordId: Long) {
        val intent = Intent(requireContext(), FormActivity::class.java)
            .putExtra(FormActivity.EXTRA_RECORD_ID, recordId)
        formLauncher.launch(intent)
    }

    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus catatan?")
            .setMessage("${records[position].vehicleName} - ${records[position].items}")
            .setPositiveButton("Hapus") { _, _ ->
                val removed = records[position]
                removed.images.forEach { ImageStore.delete(requireContext(), it) }
                records.removeAt(position)
                adapter.notifyItemRemoved(position)
                Db.saveRecords(requireContext(), records)
                SheetSync.delete(requireContext(), "records", removed.id)
                updateHeader()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun reload() {
        records.clear()
        records.addAll(Db.loadRecords(requireContext()).sortedWith(
            compareByDescending<ServiceRecord> { parseDate(it.date) }
                .thenByDescending { it.id }
        ))
        adapter.notifyDataSetChanged()
        view?.findViewById<TextView>(R.id.emptyState)?.visibility =
            if (records.isEmpty()) View.VISIBLE else View.GONE
        updateHeader()
    }

    private fun updateHeader() {
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        view?.findViewById<TextView>(R.id.textTotal)?.text =
            "Total biaya service: Rp ${nf.format(records.sumOf { it.cost })}"
    }

    private fun parseDate(date: String): Long = try {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { isLenient = false }
            .parse(date)?.time ?: Long.MIN_VALUE
    } catch (_: Exception) {
        Long.MIN_VALUE
    }
}
