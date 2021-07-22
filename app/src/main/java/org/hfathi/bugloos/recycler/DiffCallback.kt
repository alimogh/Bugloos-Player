package org.hfathi.bugloos.recycler

import androidx.recyclerview.widget.DiffUtil
import org.hfathi.bugloos.music.BaseModel

/**
 * A re-usable diff callback for all [BaseModel] implementations.
 * **Use this instead of creating a DiffCallback for each adapter.**
 * @author hamid fathi
 */
class DiffCallback<T : BaseModel> : DiffUtil.ItemCallback<T>() {
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem.id == newItem.id
    }
}
