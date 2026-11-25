package com.example.wisatajogja.ui.main.rekomendasi

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.databinding.ItemDestinationGridBinding
import com.example.wisatajogja.ui.common.DestinationDiff
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.formatRating
import com.example.wisatajogja.util.formatRupiah
import com.example.wisatajogja.util.loadPhoto

class ExploreAdapter(
    private val onClick: (Destination) -> Unit,
    private val onFavorite: (Destination) -> Unit,
) : ListAdapter<Destination, ExploreAdapter.ViewHolder>(DestinationDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDestinationGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDestinationGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Destination) = with(binding) {
            textCategory.text = item.category.uppercase()
            textTitle.text = item.name
            textArea.text = item.area
            textRating.text = "${formatRating(item.rating)} (${formatNumber(item.reviewCount)})"
            textPrice.text = formatRupiah(item.ticketPrice)
            imageDestination.loadPhoto(item.imageUrl)
            buttonFavorite.setImageResource(if (item.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            buttonFavorite.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(root.context, if (item.isFavorite) R.color.terracotta else R.color.green_dark)
            )
            root.setOnClickListener { onClick(item) }
            buttonFavorite.setOnClickListener { onFavorite(item) }
        }
    }
}
