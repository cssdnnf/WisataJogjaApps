package com.example.wisatajogja.ui.admin.pengguna

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.AdminUser
import com.example.wisatajogja.databinding.ItemAdminPenggunaBinding
import com.example.wisatajogja.util.formatNumber

class AdminUserAdapter(
    private val onMenu: (AdminUser) -> Unit,
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    private val items = mutableListOf<AdminUser>()

    fun submit(newItems: List<AdminUser>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun append(newItems: List<AdminUser>) {
        val start = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAdminPenggunaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemAdminPenggunaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AdminUser) = with(binding) {
            textInitial.text = item.name.firstOrNull()?.uppercase() ?: "W"
            textName.text = item.name
            textEmail.text = item.email
            textRole.text = if (item.role == "admin") "ADMIN" else "WISATAWAN"
            val active = item.status == "aktif"
            textStatus.text = buildString {
                append(if (active) "Aktif" else "Nonaktif")
                append(" · ")
                append(formatNumber(item.savedCount))
                append(" tersimpan · ")
                append(formatNumber(item.reservationCount))
                append(" reservasi")
            }
            textStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    root.context,
                    if (active) R.color.muted else R.color.terracotta
                )
            )
            buttonMenu.setOnClickListener { onMenu(item) }
            root.setOnClickListener { onMenu(item) }
        }
    }
}
