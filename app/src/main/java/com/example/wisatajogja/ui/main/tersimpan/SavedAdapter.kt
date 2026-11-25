package com.example.wisatajogja.ui.main.tersimpan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.databinding.ItemDestinationTersimpanBinding
import com.example.wisatajogja.ui.common.DestinationDiff
import com.example.wisatajogja.util.formatRating
import com.example.wisatajogja.util.loadPhoto

class SavedAdapter(
    private val onClick: (Destination) -> Unit,
    private val onRemove: (Destination) -> Unit,
) : ListAdapter<Destination, SavedAdapter.ViewHolder>(DestinationDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDestinationTersimpanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDestinationTersimpanBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Destination) = with(binding) {
            textCategory.text = item.category.uppercase()
            textTitle.text = item.name
            textArea.text = "${item.area} · ${item.openHours}"
            textRating.text = formatRating(item.rating)
            imageDestination.loadPhoto(item.imageUrl)
            root.setOnClickListener { onClick(item) }
            buttonRemove.setOnClickListener { onRemove(item) }
        }
    }
}
