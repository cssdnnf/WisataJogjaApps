package com.example.wisatajogja.ui.main.beranda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.databinding.ItemDestinationBerandaBinding
import com.example.wisatajogja.ui.common.DestinationDiff
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.formatRating
import com.example.wisatajogja.util.loadPhoto

class BerandaDestinationAdapter(
    private val onClick: (Destination) -> Unit,
    private val onFavorite: (Destination) -> Unit,
) : ListAdapter<Destination, BerandaDestinationAdapter.ViewHolder>(DestinationDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDestinationBerandaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDestinationBerandaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Destination) = with(binding) {
            textCategory.text = item.category.uppercase()
            textTitle.text = item.name
            textArea.text = item.area
            textRating.text = "${formatRating(item.rating)} · ${formatNumber(item.reviewCount)}"
            imageDestination.loadPhoto(item.imageUrl)
            buttonFavorite.setImageResource(
                if (item.isFavorite) com.example.wisatajogja.R.drawable.ic_favorite
                else com.example.wisatajogja.R.drawable.ic_favorite_border
            )
            buttonFavorite.imageTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(
                    root.context,
                    if (item.isFavorite) com.example.wisatajogja.R.color.terracotta
                    else com.example.wisatajogja.R.color.green_dark
                )
            )
            root.setOnClickListener { onClick(item) }
            buttonFavorite.setOnClickListener { onFavorite(item) }
        }
    }
}
