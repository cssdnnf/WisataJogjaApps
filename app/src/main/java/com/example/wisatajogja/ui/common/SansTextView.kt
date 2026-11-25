package com.example.wisatajogja.ui.common

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.wisatajogja.R

/** TextView dengan typeface Inter yang dipasang langsung agar konsisten di semua build Android. */
class SansTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        val font = ResourcesCompat.getFont(context, R.font.inter_regular)
        val style = typeface?.style ?: Typeface.NORMAL
        if (font != null) setTypeface(font, style)
    }
}
