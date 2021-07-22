package org.hfathi.bugloos.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.hfathi.bugloos.music.Album
import org.hfathi.bugloos.music.Artist
import org.hfathi.bugloos.music.BaseModel
import org.hfathi.bugloos.music.Genre
import org.hfathi.bugloos.recycler.SortMode
import org.hfathi.bugloos.settings.SettingsManager

/**
 * ViewModel that stores data for the [DetailFragment]s, such as what they're showing & what
 * [SortMode] they are currently on.
 * @author hamid fathi
 */
class DetailViewModel : ViewModel() {
    private val settingsManager = SettingsManager.getInstance()

    // --- CURRENT VALUES ---

    private val mCurrentGenre = MutableLiveData<Genre?>()
    val currentGenre: LiveData<Genre?> get() = mCurrentGenre

    private val mCurrentArtist = MutableLiveData<Artist?>()
    val currentArtist: LiveData<Artist?> get() = mCurrentArtist

    private val mCurrentAlbum = MutableLiveData<Album?>()
    val currentAlbum: LiveData<Album?> get() = mCurrentAlbum

    // --- SORT MODES ---

    private val mGenreSortMode = MutableLiveData(settingsManager.genreSortMode)
    val genreSortMode: LiveData<SortMode> get() = mGenreSortMode

    private val mArtistSortMode = MutableLiveData(settingsManager.artistSortMode)
    val albumSortMode: LiveData<SortMode> get() = mAlbumSortMode

    private val mAlbumSortMode = MutableLiveData(settingsManager.albumSortMode)
    val artistSortMode: LiveData<SortMode> get() = mArtistSortMode

    private var mIsNavigating = false
    val isNavigating: Boolean get() = mIsNavigating

    private val mNavToItem = MutableLiveData<BaseModel?>()

    /** Flag for unified navigation. Observe this to coordinate navigation to an item's UI. */
    val navToItem: LiveData<BaseModel?> get() = mNavToItem

    fun updateGenre(genre: Genre) {
        mCurrentGenre.value = genre
    }

    fun updateArtist(artist: Artist) {
        mCurrentArtist.value = artist
    }

    fun updateAlbum(album: Album) {
        mCurrentAlbum.value = album
    }

    /**
     * Increment the sort mode of the genre artists
     */
    fun incrementGenreSortMode() {
        val mode = when (mGenreSortMode.value) {
            SortMode.ALPHA_DOWN -> SortMode.ALPHA_UP
            SortMode.ALPHA_UP -> SortMode.ALPHA_DOWN

            else -> SortMode.ALPHA_DOWN
        }

        mGenreSortMode.value = mode
        settingsManager.genreSortMode = mode
    }

    /**
     * Increment the sort mode of the artist albums
     */
    fun incrementArtistSortMode() {
        val mode = when (mArtistSortMode.value) {
            SortMode.NUMERIC_DOWN -> SortMode.NUMERIC_UP
            SortMode.NUMERIC_UP -> SortMode.ALPHA_DOWN
            SortMode.ALPHA_DOWN -> SortMode.ALPHA_UP
            SortMode.ALPHA_UP -> SortMode.NUMERIC_DOWN

            else -> SortMode.NUMERIC_DOWN
        }

        mArtistSortMode.value = mode
        settingsManager.artistSortMode = mode
    }

    /**
     * Increment the sort mode of the album song
     */
    fun incrementAlbumSortMode() {
        val mode = when (mAlbumSortMode.value) {
            SortMode.NUMERIC_DOWN -> SortMode.NUMERIC_UP
            SortMode.NUMERIC_UP -> SortMode.NUMERIC_DOWN

            else -> SortMode.NUMERIC_DOWN
        }

        mAlbumSortMode.value = mode
        settingsManager.albumSortMode = mAlbumSortMode.value!!
    }

    /**
     * Navigate to an item, whether a song/album/artist
     */
    fun navToItem(item: BaseModel) {
        mNavToItem.value = item
    }

    /**
     * Mark that the navigation process is done.
     */
    fun doneWithNavToItem() {
        mNavToItem.value = null
    }

    /**
     * Update the current navigation status to [isNavigating]
     */
    fun setNavigating(isNavigating: Boolean) {
        mIsNavigating = isNavigating
    }
}
