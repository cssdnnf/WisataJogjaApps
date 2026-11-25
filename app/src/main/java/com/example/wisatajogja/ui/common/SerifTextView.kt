package com.example.wisatajogja.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.wisatajogja.R

/**
 * TextView dengan typeface Playfair Display yang dipasang langsung.
 * Resource font dari XML `android:fontFamily` tidak konsisten pada beberapa
 * build Android, sehingga heading memakai komponen ini agar tipografi stabil.
 */
class SerifTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        ResourcesCompat.getFont(context, R.font.playfair_semibold)?.let(::setTypeface)
    }
}
