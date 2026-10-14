package com.otomemo.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecordsFragment : Fragment(R.layout.fragment_records) {

    private val records = mutableListOf<ServiceRecord>()
    private val shown = mutableListOf<ServiceRecord>()
    private var vehicles: List<Vehicle> = emptyList()
    private var selectedVehicleId = -1L
    private var spinnerSig = ""
    private val current: Calendar = Calendar.getInstance()
    private val nf = NumberFormat.getInstance(Locale("in", "ID"))
    private lateinit var adapter: RecordAdapter
    private lateinit var dayAdapter: DayAdapter

    private data class DayCell(val day: Int, val total: Long, val records: List<ServiceRecord>)

    private inner class DayAdapter : RecyclerView.Adapter<DayAdapter.Holder>() {

        val cells = mutableListOf<DayCell>()

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val day: TextView = view.findViewById(R.id.textDay)
            val cost: TextView = view.findViewById(R.id.textCost)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(layoutInflater.inflate(R.layout.item_calendar_day, parent, false))

        override fun getItemCount() = cells.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cell = cells[position]
            if (cell.day <= 0) {
                holder.itemView.background = null
                holder.day.text = ""
                holder.cost.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
                return
            }
            holder.day.text = cell.day.toString()
            if (cell.total > 0) {
                holder.itemView.setBackgroundResource(R.drawable.bg_day_highlight)
                holder.cost.visibility = View.VISIBLE
                holder.cost.text = fmtShort(cell.total)
                holder.itemView.setOnClickListener { showDayDialog(cell.day, cell.records) }
            } else {
                holder.itemView.background = null
                holder.cost.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
            }
        }
    }

    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            reload()
            Toast.makeText(requireContext(), "Catatan service tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        current.set(Calendar.DAY_OF_MONTH, 1)

        val tabs = view.findViewById<TabLayout>(R.id.tabView)
        tabs.addTab(tabs.newTab().setText("List"))
        tabs.addTab(tabs.newTab().setText("Kalender"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchView(tab.position == 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        adapter = RecordAdapter(
            shown,
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

        view.findViewById<Spinner>(R.id.spinnerVehicle).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val newId = if (position <= 0) -1L
                    else vehicles.getOrNull(position - 1)?.id ?: -1L
                    if (newId != selectedVehicleId) {
                        selectedVehicleId = newId
                        refreshList()
                        refreshCalendar()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        dayAdapter = DayAdapter()
        view.findViewById<RecyclerView>(R.id.recyclerDays).apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = dayAdapter
        }
        buildWeekHeader(view)

        view.findViewById<TextView>(R.id.buttonPrev).setOnClickListener {
            current.add(Calendar.MONTH, -1)
            refreshCalendar()
        }
        view.findViewById<TextView>(R.id.buttonNext).setOnClickListener {
            current.add(Calendar.MONTH, 1)
            refreshCalendar()
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun switchView(list: Boolean) {
        view?.findViewById<LinearLayout>(R.id.listSection)?.visibility =
            if (list) View.VISIBLE else View.GONE
        view?.findViewById<LinearLayout>(R.id.calendarSection)?.visibility =
            if (list) View.GONE else View.VISIBLE
        if (!list) refreshCalendar()
    }

    private fun reload() {
        records.clear()
        records.addAll(
            Db.loadRecords(requireContext()).sortedWith(
                compareByDescending<ServiceRecord> { parseDate(it.date) }
                    .thenByDescending { it.id }
            )
        )
        vehicles = Db.loadVehicles(requireContext())
        refreshSpinner()
        refreshList()
        refreshCalendar()
    }

    private fun refreshSpinner() {
        val spinner = view?.findViewById<Spinner>(R.id.spinnerVehicle) ?: return
        val sig = vehicles.joinToString(",") { it.id.toString() }
        if (sig != spinnerSig) {
            spinnerSig = sig
            val labels = mutableListOf("Semua kendaraan")
            labels.addAll(vehicles.map { label(it) })
            spinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                labels
            )
        }
        val idx = if (selectedVehicleId < 0) 0
        else vehicles.indexOfFirst { it.id == selectedVehicleId } + 1
        if (spinner.selectedItemPosition != idx) spinner.setSelection(idx, false)
    }

    private fun label(v: Vehicle): String {
        val name = vehicleName(v)
        return if (v.plate.isNotEmpty()) "$name (${v.plate})" else name
    }

    private fun vehicleName(v: Vehicle): String =
        listOf(v.brand, v.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "-" }

    private fun matchesVehicle(r: ServiceRecord): Boolean {
        if (selectedVehicleId < 0) return true
        val v = vehicles.firstOrNull { it.id == selectedVehicleId } ?: return true
        return r.vehicleId == v.id ||
            (v.plate.isNotEmpty() && r.plateNumber == v.plate) ||
            (r.vehicleName == vehicleName(v))
    }

    private fun refreshList() {
        shown.clear()
        shown.addAll(records.filter { matchesVehicle(it) })
        adapter.notifyDataSetChanged()
        view?.findViewById<TextView>(R.id.emptyState)?.visibility =
            if (shown.isEmpty()) View.VISIBLE else View.GONE
        view?.findViewById<TextView>(R.id.textTotal)?.text =
            "Total biaya service: Rp ${nf.format(shown.sumOf { it.cost })}"
    }

    private fun refreshCalendar() {
        val view = view ?: return
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(current.time)
        view.findViewById<TextView>(R.id.textMonth).text =
            monthTitle.replaceFirstChar { it.uppercase() }

        val byDay = mutableMapOf<Int, MutableList<ServiceRecord>>()
        for (r in records.filter { matchesVehicle(it) }) {
            val c = Dates.toCalendar(r.date) ?: continue
            if (c.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == current.get(Calendar.MONTH)
            ) {
                byDay.getOrPut(c.get(Calendar.DAY_OF_MONTH)) { mutableListOf() }.add(r)
            }
        }

        val cells = mutableListOf<DayCell>()
        val firstDow = current.firstDayOfWeek
        val offset = (current.get(Calendar.DAY_OF_WEEK) - firstDow + 7) % 7
        repeat(offset) { cells.add(DayCell(-1, 0, emptyList())) }
        var monthTotal = 0L
        for (day in 1..current.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            val recs = byDay[day].orEmpty()
            val total = recs.sumOf { it.cost }
            monthTotal += total
            cells.add(DayCell(day, total, recs))
        }

        dayAdapter.cells.clear()
        dayAdapter.cells.addAll(cells)
        dayAdapter.notifyDataSetChanged()

        view.findViewById<TextView>(R.id.textMonthTotal).text =
            "Pengeluaran: Rp ${nf.format(monthTotal)}"
    }

    private fun buildWeekHeader(view: View) {
        val header = view.findViewById<LinearLayout>(R.id.weekHeader)
        header.removeAllViews()
        val symbols = DateFormatSymbols(Locale.getDefault()).shortWeekdays
        val firstDow = current.firstDayOfWeek
        for (i in 0 until 7) {
            val tv = TextView(requireContext()).apply {
                text = symbols[((firstDow - 1 + i) % 7) + 1].replace(".", "")
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#666666"))
                textSize = 12f
            }
            header.addView(
                tv,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
        }
    }

    private fun showDetail(position: Int) {
        val item = shown[position]
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
            .setMessage("${shown[position].vehicleName} - ${shown[position].items}")
            .setPositiveButton("Hapus") { _, _ ->
                val removed = shown[position]
                removed.images.forEach { ImageStore.delete(requireContext(), it) }
                records.removeAll { it.id == removed.id }
                shown.removeAt(position)
                adapter.notifyItemRemoved(position)
                Db.saveRecords(requireContext(), records)
                SheetSync.delete(requireContext(), "records", removed.id)
                view?.findViewById<TextView>(R.id.textTotal)?.text =
                    "Total biaya service: Rp ${nf.format(shown.sumOf { it.cost })}"
                refreshCalendar()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDayDialog(day: Int, dayRecords: List<ServiceRecord>) {
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(current.time)
        val message = dayRecords.joinToString("\n\n") { r ->
            buildString {
                append("• ${r.vehicleName} - ${r.items}")
                append("\nRp ${nf.format(r.cost)}")
                if (r.workshop.isNotEmpty()) append("\nBengkel: ${r.workshop}")
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("$day $monthTitle")
            .setMessage(message)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun parseDate(date: String): Long = Dates.parse(date) ?: Long.MIN_VALUE

    private fun fmtShort(v: Long): String = when {
        v <= 0L -> ""
        v >= 1_000_000L -> {
            val m = v / 1_000_000.0
            val s = if (m == kotlin.math.floor(m)) {
                m.toLong().toString()
            } else {
                String.format(Locale("in", "ID"), "%.1f", m)
            }
            "${s}jt"
        }
        v >= 1_000L -> "${v / 1_000}rb"
        else -> v.toString()
    }
}
