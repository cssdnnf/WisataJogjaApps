package com.example.wisatajogja.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.DestinationService
import com.example.wisatajogja.data.model.GalleryItem
import com.example.wisatajogja.data.model.Review
import com.example.wisatajogja.databinding.ItemGalleryBinding
import com.example.wisatajogja.databinding.ItemLayananBinding
import com.example.wisatajogja.databinding.ItemUlasanBinding
import com.example.wisatajogja.util.formatRupiah
import com.example.wisatajogja.util.loadAvatar
import com.example.wisatajogja.util.loadPhoto

class GalleryAdapter(
    private val onClick: (GalleryItem) -> Unit,
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    private val items = mutableListOf<GalleryItem>()

    fun submit(newItems: List<GalleryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemGalleryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GalleryItem) {
            binding.imageGallery.loadPhoto(item.imageUrl)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

class ServiceAdapter : RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {
    private val items = mutableListOf<DestinationService>()

    fun submit(newItems: List<DestinationService>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemLayananBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemLayananBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DestinationService) = with(binding) {
            imageIcon.setImageResource(iconRes(item.icon))
            textType.text = if (item.isCommunity) {
                root.context.getString(R.string.detail_community_type)
            } else {
                root.context.getString(R.string.detail_service_type)
            }
            textType.background = ContextCompat.getDrawable(
                root.context,
                if (item.isCommunity) R.drawable.bg_status_draf else R.drawable.bg_status_terbit
            )
            textName.text = item.name
            textDescription.text = item.description
            textPrice.text = if (item.price <= 0) "Tanpa biaya" else "${formatRupiah(item.price)} / grup"
        }

        private fun iconRes(icon: String): Int = when (icon) {
            "jeep" -> R.drawable.ic_directions_car
            "food" -> R.drawable.ic_restaurant
            "homestay" -> R.drawable.ic_hotel
            "camera" -> R.drawable.ic_camera_alt
            "beach" -> R.drawable.ic_beach_access
            "museum" -> R.drawable.ic_museum
            "hiking" -> R.drawable.ic_hiking
            "groups" -> R.drawable.ic_group
            else -> R.drawable.ic_local_guide
        }
    }
}

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {
    private val items = mutableListOf<Review>()

    fun submit(newItems: List<Review>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemUlasanBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemUlasanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Review) = with(binding) {
            textAuthor.text = item.authorName
            textComment.text = item.comment
            textDate.text = item.date
            textStars.text = "★".repeat(item.rating.coerceIn(0, 5)) + "☆".repeat(5 - item.rating.coerceIn(0, 5))
            if (item.authorPhotoUrl.isNullOrBlank()) {
                imageAuthor.visibility = View.GONE
                textAuthorInitial.visibility = View.VISIBLE
                textAuthorInitial.text = item.authorName.firstOrNull()?.uppercase() ?: "W"
            } else {
                textAuthorInitial.visibility = View.GONE
                imageAuthor.visibility = View.VISIBLE
                imageAuthor.loadAvatar(item.authorPhotoUrl)
            }
        }
    }
}
