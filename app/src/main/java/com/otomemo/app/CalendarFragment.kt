package com.otomemo.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val current: Calendar = Calendar.getInstance()
    private val nf = NumberFormat.getInstance(Locale("in", "ID"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        current.set(Calendar.DAY_OF_MONTH, 1)
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

    private fun rebuild() {
        val view = view ?: return
        val density = resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }
        val primary = ContextCompat.getColor(requireContext(), R.color.primary)
        val highlight = ColorDrawable(Color.argb(36, Color.red(primary), Color.green(primary), Color.blue(primary)))
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(current.time)

        view.findViewById<TextView>(R.id.textMonth).text = monthTitle.replaceFirstChar { it.uppercase() }

        val records = Db.loadRecords(requireContext())
        val byDay = mutableMapOf<Int, MutableList<ServiceRecord>>()
        for (r in records) {
            val c = parseCal(r.date) ?: continue
            if (c.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == current.get(Calendar.MONTH)
            ) {
                byDay.getOrPut(c.get(Calendar.DAY_OF_MONTH)) { mutableListOf() }.add(r)
            }
        }

        val grid = view.findViewById<GridLayout>(R.id.gridCalendar)
        grid.removeAllViews()
        grid.columnCount = 7

        val symbols = DateFormatSymbols(Locale.getDefault()).shortWeekdays
        val firstDow = current.getFirstDayOfWeek()
        for (i in 0 until 7) {
            val header = TextView(requireContext()).apply {
                text = symbols[((firstDow - 1 + i) % 7) + 1].replace(".", "")
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#666666"))
                textSize = 12f
                minimumHeight = dp(32)
                setPadding(dp(2), dp(4), dp(2), dp(4))
                layoutParams = cellParams()
            }
            grid.addView(header)
        }

        val offset = (current.get(Calendar.DAY_OF_WEEK) - firstDow + 7) % 7
        repeat(offset) {
            val pad = View(requireContext()).apply { layoutParams = cellParams() }
            grid.addView(pad)
        }

        var monthTotal = 0L
        for (day in 1..current.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            val dayRecords = byDay[day].orEmpty()
            val dayTotal = dayRecords.sumOf { it.cost }
            monthTotal += dayTotal

            val cell = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                minimumHeight = dp(52)
                setPadding(dp(2), dp(4), dp(2), dp(4))
                layoutParams = cellParams()
                if (dayTotal > 0) background = highlight
            }
            val label = TextView(requireContext()).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(Color.parseColor("#333333"))
            }
            cell.addView(label)
            if (dayTotal > 0) {
                val cost = TextView(requireContext()).apply {
                    text = fmtShort(dayTotal)
                    gravity = Gravity.CENTER
                    textSize = 10f
                    setTextColor(primary)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                cell.addView(cost)
                cell.setOnClickListener { showDayDialog(day, monthTitle, dayRecords) }
            }
            grid.addView(cell)
        }

        view.findViewById<TextView>(R.id.textTotal).text =
            "Pengeluaran: Rp ${nf.format(monthTotal)}"
    }

    private fun cellParams(): GridLayout.LayoutParams {
        val p = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        )
        p.setMargins(1, 1, 1, 1)
        return p
    }

    private fun showDayDialog(day: Int, monthTitle: String, records: List<ServiceRecord>) {
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

    private fun parseCal(date: String): Calendar? {
        return try {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            fmt.isLenient = false
            val d = fmt.parse(date)
            if (d == null) null else Calendar.getInstance().apply { time = d }
        } catch (_: Exception) {
            null
        }
    }
}
