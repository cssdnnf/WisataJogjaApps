package com.example.wisatajogja.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.example.wisatajogja.data.model.Destination

object DestinationDiff : DiffUtil.ItemCallback<Destination>() {
    override fun areItemsTheSame(oldItem: Destination, newItem: Destination): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Destination, newItem: Destination): Boolean =
        oldItem == newItem
}
