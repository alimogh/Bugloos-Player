package org.hfathi.bugloos.settings.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.hfathi.bugloos.R
import androidx.preference.R as prefR

class IntListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = prefR.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    val entries: Array<CharSequence>
    val values: IntArray

    private var currentValue: Int? = null
    private val defValue: Int

    init {
        val prefAttrs = context.obtainStyledAttributes(
            attrs, R.styleable.IntListPreference, defStyleAttr, defStyleRes
        )

        entries = prefAttrs.getTextArray(R.styleable.IntListPreference_entries)

        values = context.resources.getIntArray(
            prefAttrs.getResourceId(R.styleable.IntListPreference_entryValues, -1)
        )

        defValue = prefAttrs.getInt(prefR.styleable.Preference_defaultValue, Int.MIN_VALUE)

        prefAttrs.recycle()

        summaryProvider = IntListSummaryProvider()
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int) = a.getInt(index, -1)

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)

        if (defaultValue != null) {
            // If were given a default value, we need to assign it.
            setValue(defaultValue as Int)
        } else {
            currentValue = getPersistedInt(defValue)
        }
    }

    fun getValueIndex(): Int {
        val curValue = currentValue

        if (curValue != null) {
            return values.indexOf(curValue)
        }

        return -1
    }

    /**
     * Set a value using the index of it in [values]
     */
    fun setValueIndex(index: Int) {
        setValue(values[index])
    }

    private fun setValue(value: Int) {
        if (value != currentValue) {
            currentValue = value

            callChangeListener(value)
            persistInt(value)
            notifyChanged()
        }
    }

    private inner class IntListSummaryProvider : SummaryProvider<IntListPreference> {
        override fun provideSummary(preference: IntListPreference?): CharSequence {
            val index = getValueIndex()

            if (index != -1) {
                return entries[index]
            }

            return context.getString(prefR.string.not_set)
        }
    }
}
