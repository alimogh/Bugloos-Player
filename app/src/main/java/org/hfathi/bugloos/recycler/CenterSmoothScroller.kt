package org.hfathi.bugloos.recycler

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

/**
 * [LinearSmoothScroller] subclass that centers the item on the screen instead of snapping to the
 * top or bottom.
 * @author hamid fathi
 */
class CenterSmoothScroller(context: Context, target: Int) : LinearSmoothScroller(context) {
    init {
        targetPosition = target
    }

    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int {
        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
    }
}
