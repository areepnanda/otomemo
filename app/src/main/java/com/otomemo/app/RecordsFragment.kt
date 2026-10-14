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
    private lateinit var dayExpenseAdapter: DayExpenseAdapter
    private val dayRecords = mutableMapOf<Int, MutableList<ServiceRecord>>()

    private data class DayCell(val day: Int, val total: Long, val records: List<ServiceRecord>)

    private inner class DayAdapter : RecyclerView.Adapter<DayAdapter.Holder>() {

        val cells = mutableListOf<DayCell>()
        var selectedDay = 0

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val day: TextView = view.findViewById(R.id.textDay)
            val dot: View = view.findViewById(R.id.dotExpense)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(layoutInflater.inflate(R.layout.item_calendar_day, parent, false))

        override fun getItemCount() = cells.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cell = cells[position]
            if (cell.day <= 0) {
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
                holder.day.background = null
                holder.day.text = ""
                holder.dot.visibility = View.GONE
                return
            }
            holder.day.text = cell.day.toString()
            when {
                cell.day == selectedDay -> {
                    holder.day.setBackgroundResource(R.drawable.bg_day_selected)
                    holder.day.setTextColor(Color.WHITE)
                }
                isToday(cell.day) -> {
                    holder.day.setBackgroundResource(R.drawable.bg_day_today)
                    holder.day.setTextColor(Color.parseColor("#00796B"))
                }
                else -> {
                    holder.day.background = null
                    holder.day.setTextColor(Color.parseColor("#333333"))
                }
            }
            holder.dot.visibility = if (cell.total > 0) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                selectedDay = if (selectedDay == cell.day) 0 else cell.day
                notifyDataSetChanged()
                updateDayPanel()
            }
        }
    }

    private inner class DayExpenseAdapter : RecyclerView.Adapter<DayExpenseAdapter.Holder>() {

        val rows = mutableListOf<ServiceRecord>()

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val vehicle: TextView = view.findViewById(R.id.textVehicle)
            val detail: TextView = view.findViewById(R.id.textItems)
            val cost: TextView = view.findViewById(R.id.textCost)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(layoutInflater.inflate(R.layout.item_day_expense, parent, false))

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val r = rows[position]
            holder.vehicle.text = r.vehicleName
            holder.detail.text = listOfNotNull(
                r.items.ifEmpty { null },
                r.workshop.ifEmpty { null }
            ).joinToString("  •  ")
            holder.cost.text = "Rp ${nf.format(r.cost)}"
            holder.itemView.setOnClickListener { showDetail(r) }
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
            onClick = { position -> if (position in shown.indices) showDetail(shown[position]) },
            onLongClick = { position ->
                if (position in shown.indices) confirmDelete(shown[position])
            }
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
        dayExpenseAdapter = DayExpenseAdapter()
        view.findViewById<RecyclerView>(R.id.recyclerDayExpenses).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dayExpenseAdapter
        }
        buildWeekHeader(view)

        view.findViewById<TextView>(R.id.buttonPrev).setOnClickListener {
            current.add(Calendar.MONTH, -1)
            dayAdapter.selectedDay = 0
            refreshCalendar()
        }
        view.findViewById<TextView>(R.id.buttonNext).setOnClickListener {
            current.add(Calendar.MONTH, 1)
            dayAdapter.selectedDay = 0
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

        dayRecords.clear()
        var monthTotal = 0L
        for (r in records.filter { matchesVehicle(it) }) {
            val c = Dates.toCalendar(r.date) ?: continue
            if (c.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == current.get(Calendar.MONTH)
            ) {
                dayRecords.getOrPut(c.get(Calendar.DAY_OF_MONTH)) { mutableListOf() }.add(r)
                monthTotal += r.cost
            }
        }

        val cells = mutableListOf<DayCell>()
        val firstDow = current.firstDayOfWeek
        val offset = (current.get(Calendar.DAY_OF_WEEK) - firstDow + 7) % 7
        repeat(offset) { cells.add(DayCell(-1, 0, emptyList())) }
        for (day in 1..current.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            val recs = dayRecords[day].orEmpty()
            cells.add(DayCell(day, recs.sumOf { it.cost }, recs))
        }

        dayAdapter.cells.clear()
        dayAdapter.cells.addAll(cells)
        dayAdapter.notifyDataSetChanged()

        view.findViewById<TextView>(R.id.textMonthTotal).text =
            "Rp ${nf.format(monthTotal)}"
        view.findViewById<TextView>(R.id.textMonthCount).text =
            "${dayRecords.values.sumOf { it.size }} catatan"
        updateDayPanel()
    }

    private fun updateDayPanel() {
        val view = view ?: return
        val header = view.findViewById<TextView>(R.id.textDayHeader)
        val empty = view.findViewById<TextView>(R.id.textDayEmpty)
        val list = view.findViewById<RecyclerView>(R.id.recyclerDayExpenses)
        val day = if (::dayAdapter.isInitialized) dayAdapter.selectedDay else 0
        if (day <= 0) {
            header.text = "Pengeluaran harian"
            empty.text = "Ketuk tanggal untuk melihat pengeluaran"
            empty.visibility = View.VISIBLE
            list.visibility = View.GONE
        } else {
            val cal = current.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, day)
            header.text =
                SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(cal.time)
                    .replaceFirstChar { it.uppercase() }
            val recs = dayRecords[day].orEmpty()
            if (recs.isEmpty()) {
                empty.text = "Tidak ada pengeluaran di tanggal ini"
                empty.visibility = View.VISIBLE
                list.visibility = View.GONE
            } else {
                empty.visibility = View.GONE
                list.visibility = View.VISIBLE
            }
            dayExpenseAdapter.rows.clear()
            dayExpenseAdapter.rows.addAll(recs)
            dayExpenseAdapter.notifyDataSetChanged()
        }
    }

    private fun isToday(day: Int): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == current.get(Calendar.MONTH) &&
            now.get(Calendar.DAY_OF_MONTH) == day
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

    private fun showDetail(item: ServiceRecord) {
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
            .setNegativeButton("Hapus") { _, _ -> confirmDelete(item) }
            .show()
    }

    private fun openEdit(recordId: Long) {
        val intent = Intent(requireContext(), FormActivity::class.java)
            .putExtra(FormActivity.EXTRA_RECORD_ID, recordId)
        formLauncher.launch(intent)
    }

    private fun confirmDelete(item: ServiceRecord) {
        val position = shown.indexOfFirst { it.id == item.id }
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus catatan?")
            .setMessage("${item.vehicleName} - ${item.items}")
            .setPositiveButton("Hapus") { _, _ ->
                item.images.forEach { ImageStore.delete(requireContext(), it) }
                records.removeAll { it.id == item.id }
                if (position >= 0) {
                    shown.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
                Db.saveRecords(requireContext(), records)
                SheetSync.delete(requireContext(), "records", item.id)
                view?.findViewById<TextView>(R.id.textTotal)?.text =
                    "Total biaya service: Rp ${nf.format(shown.sumOf { it.cost })}"
                refreshCalendar()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun parseDate(date: String): Long = Dates.parse(date) ?: Long.MIN_VALUE
}
