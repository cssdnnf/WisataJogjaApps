package com.example.wisatajogja.ui.navigation

import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

data class BottomNavItem(
    val containerId: Int,
    val iconId: Int,
    val labelId: Int,
)

class BottomNavController(
    root: View,
    private val items: List<BottomNavItem>,
    private val activeBackground: Int,
    private val activeContentColor: Int,
    private val inactiveContentColor: Int,
    private val onItemSelected: (Int) -> Unit,
) {
    private val containers = items.map { root.findViewById<LinearLayout>(it.containerId) }
    private val icons = items.map { root.findViewById<ImageView>(it.iconId) }
    private val labels = items.map { root.findViewById<TextView>(it.labelId) }

    init {
        containers.forEachIndexed { index, container ->
            container.setOnClickListener { onItemSelected(index) }
        }
    }

    fun select(index: Int) {
        containers.forEachIndexed { position, container ->
            val selected = position == index
            container.setBackgroundResource(if (selected) activeBackground else com.example.wisatajogja.R.drawable.bg_nav_transparent)
            container.isSelected = selected
            val params = container.layoutParams as LinearLayout.LayoutParams
            params.weight = if (selected) 1.65f else 1f
            container.layoutParams = params

            val color = ContextCompat.getColor(
                container.context,
                if (selected) activeContentColor else inactiveContentColor,
            )
            icons[position].setColorFilter(color)
            labels[position].setTextColor(color)
            labels[position].setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
