package com.example.wisatajogja.ui.reservasi

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Reservation
import com.example.wisatajogja.databinding.ItemReservasiSayaBinding
import com.example.wisatajogja.util.formatRupiah
import com.example.wisatajogja.util.formatVisitDate
import com.example.wisatajogja.util.loadPhoto

class ReservationHistoryAdapter : ListAdapter<Reservation, ReservationHistoryAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemReservasiSayaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemReservasiSayaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Reservation) = with(binding) {
            imageDestination.loadPhoto(item.imageUrl)
            textCode.text = item.code
            textDestination.text = item.destinationName
            textArea.text = item.area
            textDate.text = formatVisitDate(item.visitDate)
            textPeople.text = root.context.getString(R.string.reservation_people_value, item.people)
            textTotal.text = formatRupiah(item.totalPrice)
            textStatus.text = statusLabel(item.status, root.context)
            val colors = statusColors(item.status, root.context)
            textStatus.background = GradientDrawable().apply {
                cornerRadius = 18f * root.resources.displayMetrics.density
                setColor(colors.first)
            }
            textStatus.setTextColor(colors.second)
            textNotes.visibility = if (item.notes.isBlank()) View.GONE else View.VISIBLE
            textNotes.text = item.notes
        }

        private fun statusLabel(status: String, context: android.content.Context): String = when (status) {
            "dikonfirmasi" -> context.getString(R.string.reservation_status_confirmed)
            "selesai" -> context.getString(R.string.reservation_status_done)
            "dibatalkan" -> context.getString(R.string.reservation_status_cancelled)
            else -> context.getString(R.string.reservation_status_waiting)
        }

        private fun statusColors(status: String, context: android.content.Context): Pair<Int, Int> = when (status) {
            "dikonfirmasi" -> ContextCompat.getColor(context, R.color.green_soft) to ContextCompat.getColor(context, R.color.green_dark)
            "selesai" -> ContextCompat.getColor(context, R.color.cream_deep) to ContextCompat.getColor(context, R.color.muted)
            "dibatalkan" -> ContextCompat.getColor(context, R.color.cream_deep) to ContextCompat.getColor(context, R.color.terracotta)
            else -> ContextCompat.getColor(context, R.color.gold_light) to ContextCompat.getColor(context, R.color.green_dark)
        }
    }

    private object Diff : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(oldItem: Reservation, newItem: Reservation): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Reservation, newItem: Reservation): Boolean = oldItem == newItem
    }
}
