package com.otomemo.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val current: Calendar = Calendar.getInstance()
    private val nf = NumberFormat.getInstance(Locale("in", "ID"))
    private lateinit var adapter: DayAdapter

    private data class DayCell(val day: Int, val total: Long, val records: List<ServiceRecord>)

    private inner class DayAdapter : RecyclerView.Adapter<DayAdapter.Holder>() {

        val cells = mutableListOf<DayCell>()

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        current.set(Calendar.DAY_OF_MONTH, 1)
        adapter = DayAdapter()
        view.findViewById<RecyclerView>(R.id.recyclerDays).apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = this@CalendarFragment.adapter
        }
        buildWeekHeader(view)
        view.findViewById<TextView>(R.id.buttonPrev).setOnClickListener {
            current.add(Calendar.MONTH, -1)
            rebuild()
        }
        view.findViewById<TextView>(R.id.buttonNext).setOnClickListener {
            current.add(Calendar.MONTH, 1)
            rebuild()
        }
        rebuild()
    }

    override fun onResume() {
        super.onResume()
        rebuild()
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

    private fun rebuild() {
        val view = view ?: return
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(current.time)
        view.findViewById<TextView>(R.id.textMonth).text = monthTitle.replaceFirstChar { it.uppercase() }

        val byDay = mutableMapOf<Int, MutableList<ServiceRecord>>()
        for (r in Db.loadRecords(requireContext())) {
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

        adapter.cells.clear()
        adapter.cells.addAll(cells)
        adapter.notifyDataSetChanged()

        view.findViewById<TextView>(R.id.textTotal).text =
            "Pengeluaran: Rp ${nf.format(monthTotal)}"
    }

    private fun showDayDialog(day: Int, records: List<ServiceRecord>) {
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(current.time)
        val message = records.joinToString("\n\n") { r ->
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
