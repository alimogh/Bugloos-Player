package org.hfathi.bugloos.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.Spanned
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.text.HtmlCompat
import org.hfathi.bugloos.R

/**
 * A list of all possible accents.
 * TODO: Add custom accents
 */
val ACCENTS = arrayOf(
    Accent(R.color.red, R.style.Theme_Red, R.style.Theme_Red_Black, R.string.color_label_red),
    Accent(R.color.pink, R.style.Theme_Pink, R.style.Theme_Pink_Black, R.string.color_label_pink),
    Accent(
        R.color.purple,
        R.style.Theme_Purple,
        R.style.Theme_Purple_Black,
        R.string.color_label_purple
    ),
    Accent(
        R.color.deep_purple,
        R.style.Theme_DeepPurple,
        R.style.Theme_DeepPurple_Black,
        R.string.color_label_deep_purple
    ),
    Accent(
        R.color.indigo,
        R.style.Theme_Indigo,
        R.style.Theme_Indigo_Black,
        R.string.color_label_indigo
    ),
    Accent(R.color.blue, R.style.Theme_Blue, R.style.Theme_Blue_Black, R.string.color_label_blue),
    Accent(
        R.color.light_blue,
        R.style.Theme_LightBlue,
        R.style.Theme_LightBlue_Black,
        R.string.color_label_light_blue
    ),
    Accent(R.color.cyan, R.style.Theme_Cyan, R.style.Theme_Cyan_Black, R.string.color_label_cyan),
    Accent(R.color.teal, R.style.Theme_Teal, R.style.Theme_Teal_Black, R.string.color_label_teal),
    Accent(R.color.green, R.style.Theme_Green, R.style.Theme_Green_Black, R.string.color_label_green),
    Accent(
        R.color.light_green,
        R.style.Theme_LightGreen,
        R.style.Theme_LightGreen_Black,
        R.string.color_label_light_green
    ),
    Accent(R.color.lime, R.style.Theme_Lime, R.style.Theme_Lime_Black, R.string.color_label_lime),
    Accent(
        R.color.yellow,
        R.style.Theme_Yellow,
        R.style.Theme_Yellow_Black,
        R.string.color_label_yellow
    ),
    Accent(
        R.color.orange,
        R.style.Theme_Orange,
        R.style.Theme_Orange_Black,
        R.string.color_label_orange
    ),
    Accent(
        R.color.deep_orange,
        R.style.Theme_DeepOrange,
        R.style.Theme_DeepOrange_Black,
        R.string.color_label_deep_orange
    ),
    Accent(R.color.brown, R.style.Theme_Brown, R.style.Theme_Brown_Black, R.string.color_label_brown),
    Accent(R.color.grey, R.style.Theme_Grey, R.style.Theme_Grey_Black, R.string.color_label_grey),
    Accent(
        R.color.blue_grey,
        R.style.Theme_BlueGrey,
        R.style.Theme_BlueGrey_Black,
        R.string.color_label_blue_grey
    ),
)

/**
 * The data object for an accent.
 * @property color The color resource for this accent
 * @property theme The theme resource for this accent
 * @property blackTheme The black theme resource for this accent
 * @property name  The name of this accent
 * @author OxygenCobalt
 */
data class Accent(
    @ColorRes val color: Int,
    @StyleRes val theme: Int,
    @StyleRes val blackTheme: Int,
    @StringRes val name: Int
) {
    /**
     * Get a [ColorStateList] of the accent
     */
    fun getStateList(context: Context) = color.toStateList(context)

    /**
     * Get the name (in bold) and the hex value of a accent.
     */
    @SuppressLint("ResourceType")
    fun getDetailedSummary(context: Context): Spanned {
        val name = context.getString(name)
        val hex = context.getString(color).uppercase()

        return HtmlCompat.fromHtml(
            context.getString(R.string.format_accent_summary, name, hex),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }

    companion object {
        @Volatile
        private var CURRENT: Accent? = null

        /**
         * Get the current accent.
         * @return The current accent
         * @throws IllegalStateException When the accent has not been set.
         */
        fun get(): Accent {
            val cur = CURRENT

            if (cur != null) {
                return cur
            }

            error("Accent must be set before retrieving it.")
        }

        /**
         * Set the current accent.
         * @return The new accent
         */
        fun set(accent: Accent): Accent {
            synchronized(this) {
                CURRENT = accent
            }

            return accent
        }
    }
}
