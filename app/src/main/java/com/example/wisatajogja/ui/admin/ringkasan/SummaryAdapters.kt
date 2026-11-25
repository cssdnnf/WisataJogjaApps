package com.example.wisatajogja.ui.admin.ringkasan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.data.model.AdminUser
import com.example.wisatajogja.data.model.PopularDestination
import com.example.wisatajogja.data.model.RecentReservation
import com.example.wisatajogja.databinding.ItemAdminPenggunaTerbaruBinding
import com.example.wisatajogja.databinding.ItemAdminPopulerBinding
import com.example.wisatajogja.databinding.ItemAdminReservasiBinding
import com.example.wisatajogja.util.formatDateShort
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.loadPhoto

class PopularDestinationAdapter : RecyclerView.Adapter<PopularDestinationAdapter.ViewHolder>() {
    private val items = mutableListOf<PopularDestination>()

    fun submit(newItems: List<PopularDestination>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAdminPopulerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    inner class ViewHolder(private val binding: ItemAdminPopulerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PopularDestination, rank: Int) = with(binding) {
            textRank.text = rank.toString()
            textName.text = item.name
            textArea.text = item.area
            textSaved.text = NumberFormatCompact.compact(item.savedCount)
            imageDestination.loadPhoto(item.imageUrl)
        }
    }
}

class RecentUsersAdapter : RecyclerView.Adapter<RecentUsersAdapter.ViewHolder>() {
    private val items = mutableListOf<AdminUser>()

    fun submit(newItems: List<AdminUser>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAdminPenggunaTerbaruBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemAdminPenggunaTerbaruBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AdminUser) = with(binding) {
            textInitial.text = item.name.firstOrNull()?.uppercase() ?: "W"
            textName.text = item.name
            textEmail.text = item.email
            textDate.text = formatDateShort(item.createdAt)
        }
    }
}

class RecentReservationsAdapter : RecyclerView.Adapter<RecentReservationsAdapter.ViewHolder>() {
    private val items = mutableListOf<RecentReservation>()

    fun submit(newItems: List<RecentReservation>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAdminReservasiBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemAdminReservasiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecentReservation) = with(binding) {
            textCode.text = item.code
            textMeta.text = "${item.userName} · ${item.destinationName}"
            textStatus.text = statusLabel(item.status)
            textStatus.background = root.context.getDrawable(
                when (item.status) {
                    "dikonfirmasi" -> com.example.wisatajogja.R.drawable.bg_status_terbit
                    "dibatalkan" -> com.example.wisatajogja.R.drawable.bg_status_nonaktif
                    else -> com.example.wisatajogja.R.drawable.bg_status_draf
                }
            )
        }

        private fun statusLabel(status: String): String = when (status) {
            "dikonfirmasi" -> "Dikonfirmasi"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> "Menunggu"
        }
    }
}

object NumberFormatCompact {
    fun compact(value: Int): String = if (value >= 1000) "${formatNumber(value)}" else value.toString()
}
