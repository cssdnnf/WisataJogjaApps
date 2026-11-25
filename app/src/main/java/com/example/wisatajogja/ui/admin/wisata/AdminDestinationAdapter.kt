package com.example.wisatajogja.ui.admin.wisata

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.databinding.ItemAdminWisataBinding
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.loadPhoto

class AdminDestinationAdapter(
    private val onEdit: (Destination) -> Unit,
    private val onDelete: (Destination) -> Unit,
) : RecyclerView.Adapter<AdminDestinationAdapter.ViewHolder>() {

    private val items = mutableListOf<Destination>()

    fun submit(newItems: List<Destination>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAdminWisataBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemAdminWisataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Destination) = with(binding) {
            textName.text = item.name
            textMeta.text = "${item.category} · ${item.area} · ${item.openHours}"
            textSaved.text = "${formatNumber(item.savedCount)} tersimpan"
            textStatus.text = if (item.status == "terbit") "Terbit" else "Draf"
            textStatus.background = root.context.getDrawable(
                if (item.status == "terbit") R.drawable.bg_status_terbit else R.drawable.bg_status_draf
            )
            textStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    root.context,
                    if (item.status == "terbit") R.color.green_dark else R.color.green_dark
                )
            )
            imageDestination.loadPhoto(item.imageUrl)
            root.setOnClickListener { onEdit(item) }
            buttonEdit.setOnClickListener { onEdit(item) }
            buttonDelete.setOnClickListener { onDelete(item) }
        }
    }
}
