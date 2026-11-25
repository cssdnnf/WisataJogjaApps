package com.example.wisatajogja.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.wisatajogja.R

fun ImageView.loadPhoto(url: String?, circular: Boolean = false) {
    val builder = Glide.with(this)
        .load(url)
        .placeholder(R.drawable.bg_image_placeholder)
        .error(R.drawable.bg_image_placeholder)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .transition(DrawableTransitionOptions.withCrossFade(220))
    if (circular) builder.circleCrop()
    builder.centerCrop().into(this)
}

fun ImageView.loadAvatar(url: String?) {
    val builder = Glide.with(this)
        .load(url)
        .placeholder(R.drawable.bg_avatar_placeholder)
        .error(R.drawable.bg_avatar_placeholder)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .circleCrop()
    builder.into(this)
}
