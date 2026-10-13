package com.otomemo.app

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class RecordAdapter(
    private val items: List<ServiceRecord>,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<RecordAdapter.Holder>() {

    class Holder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textTitle)
        val detail: TextView = view.findViewById(R.id.textDetail)
        val cost: TextView = view.findViewById(R.id.textCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = "${item.vehicleName}  •  ${item.plateNumber}".trim()
        holder.detail.text = buildString {
            append(item.date)
            if (item.workshop.isNotEmpty()) append("  •  ${item.workshop}")
            append("\n")
            append(item.items)
            if (item.notes.isNotEmpty()) append("\nCatatan: ${item.notes}")
        }
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        holder.cost.text = "Rp ${nf.format(item.cost)}"
        holder.itemView.setOnLongClickListener {
            onLongClick(holder.bindingAdapterPosition)
            true
        }
    }
}
