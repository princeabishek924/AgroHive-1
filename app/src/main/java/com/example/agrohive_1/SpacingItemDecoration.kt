package com.example.agrohive_1

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as? GridLayoutManager
        val position = parent.getChildAdapterPosition(view)
        val spanCount = layoutManager?.spanCount ?: 1

        with(outRect) {
            // Add spacing on all sides
            left = spacing
            right = spacing
            top = spacing
            bottom = spacing

            // Adjust for the first item in a row (no left spacing) and last item (no right spacing)
            if (position % spanCount == 0) {
                left = 0
            }
            if (position % spanCount == spanCount - 1) {
                right = 0
            }

            // Adjust for the first row (no top spacing)
            if (position < spanCount) {
                top = 0
            }
        }
    }
}