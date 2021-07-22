package org.hfathi.bugloos.search

import android.content.Context
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.hfathi.bugloos.R
import org.hfathi.bugloos.music.BaseModel
import org.hfathi.bugloos.music.Header
import org.hfathi.bugloos.music.MusicStore
import org.hfathi.bugloos.recycler.DisplayMode
import org.hfathi.bugloos.settings.SettingsManager

/**
 * The [ViewModel] for the search functionality
 * @author hamid fathi
 */
class SearchViewModel : ViewModel() {
    private val mSearchResults = MutableLiveData(listOf<BaseModel>())
    private var mIsNavigating = false
    private var mFilterMode = DisplayMode.SHOW_ALL
    private var mLastQuery = ""

    /** Current search results from the last [doSearch] call. */
    val searchResults: LiveData<List<BaseModel>> get() = mSearchResults
    val isNavigating: Boolean get() = mIsNavigating
    val filterMode: DisplayMode get() = mFilterMode

    private val musicStore = MusicStore.getInstance()
    private val settingsManager = SettingsManager.getInstance()

    init {
        mFilterMode = settingsManager.searchFilterMode
    }

    /**
     * Use [query] to perform a search of the music library.
     * Will push results to [searchResults].
     */
    fun doSearch(query: String, context: Context) {
        mLastQuery = query

        if (query.isEmpty()) {
            mSearchResults.value = listOf()

            return
        }

        viewModelScope.launch {
            val results = mutableListOf<BaseModel>()

            if (mFilterMode.isAllOr(DisplayMode.SHOW_ARTISTS)) {
                musicStore.artists.filterByOrNull(query)?.let { artists ->
                    results.add(Header(id = -2, name = context.getString(R.string.label_artists)))
                    results.addAll(artists)
                }
            }

            if (mFilterMode.isAllOr(DisplayMode.SHOW_ALBUMS)) {
                musicStore.albums.filterByOrNull(query)?.let { albums ->
                    results.add(Header(id = -3, name = context.getString(R.string.label_albums)))
                    results.addAll(albums)
                }
            }

            if (mFilterMode.isAllOr(DisplayMode.SHOW_GENRES)) {
                musicStore.genres.filterByOrNull(query)?.let { genres ->
                    results.add(Header(id = -4, name = context.getString(R.string.label_genres)))
                    results.addAll(genres)
                }
            }

            if (mFilterMode.isAllOr(DisplayMode.SHOW_SONGS)) {
                musicStore.songs.filterByOrNull(query)?.let { songs ->
                    results.add(Header(id = -5, name = context.getString(R.string.label_songs)))
                    results.addAll(songs)
                }
            }

            mSearchResults.value = results
        }
    }

    /**
     * Update the current filter mode with a menu [id].
     * New value will be pushed to [filterMode].
     */
    fun updateFilterModeWithId(@IdRes id: Int, context: Context) {
        mFilterMode = DisplayMode.fromId(id)

        settingsManager.searchFilterMode = mFilterMode

        doSearch(mLastQuery, context)
    }

    /**
     * Shortcut that will run a ignoreCase filter on a list and only return
     * a value if the resulting list is empty.
     */
    private fun List<BaseModel>.filterByOrNull(value: String): List<BaseModel>? {
        val filtered = filter {
            it.name.contains(value, ignoreCase = true)
        }

        return if (filtered.isNotEmpty()) filtered else null
    }

    /**
     * Update the current navigation status to [isNavigating]
     */
    fun setNavigating(isNavigating: Boolean) {
        mIsNavigating = isNavigating
    }
}
