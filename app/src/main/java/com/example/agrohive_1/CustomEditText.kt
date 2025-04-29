package com.example.agrohive_1

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.widget.AppCompatEditText

class CustomEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    override fun performClick(): Boolean {
        // Notify accessibility services of the click event
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        // Call the default click handler (if any)
        return super.performClick()
    }
}