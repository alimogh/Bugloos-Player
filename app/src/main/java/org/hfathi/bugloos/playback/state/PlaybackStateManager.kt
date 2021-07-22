package org.hfathi.bugloos.playback.state

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hfathi.bugloos.database.PlaybackState
import org.hfathi.bugloos.database.PlaybackStateDatabase
import org.hfathi.bugloos.database.QueueItem
import org.hfathi.bugloos.logD
import org.hfathi.bugloos.logE
import org.hfathi.bugloos.music.Album
import org.hfathi.bugloos.music.Artist
import org.hfathi.bugloos.music.Genre
import org.hfathi.bugloos.music.MusicStore
import org.hfathi.bugloos.music.Parent
import org.hfathi.bugloos.music.Song
import org.hfathi.bugloos.settings.SettingsManager

/**
 * Master class (and possible god object) for the playback state.
 *
 * This should ***NOT*** be used outside of the playback module.
 * - If you want to use the playback state in the UI, use [org.hfathi.bugloos.playback.PlaybackViewModel] as it can withstand volatile UIs.
 * - If you want to use the playback state with the ExoPlayer instance or system-side things, use [org.hfathi.bugloos.playback.system.PlaybackService].
 *
 * All access should be done with [PlaybackStateManager.getInstance].
 * @author hamid fathi
 */
class PlaybackStateManager private constructor() {
    // Playback
    private var mSong: Song? = null
        set(value) {
            field = value
            callbacks.forEach { it.onSongUpdate(value) }
        }
    private var mPosition: Long = 0
        set(value) {
            field = value
            callbacks.forEach { it.onPositionUpdate(value) }
        }
    private var mParent: Parent? = null
        set(value) {
            field = value
            callbacks.forEach { it.onParentUpdate(value) }
        }

    // Queue
    private var mQueue = mutableListOf<Song>()
        set(value) {
            field = value
            callbacks.forEach { it.onQueueUpdate(value) }
        }
    private var mUserQueue = mutableListOf<Song>()
        set(value) {
            field = value
            callbacks.forEach { it.onUserQueueUpdate(value) }
        }
    private var mIndex = 0
        set(value) {
            field = value
            callbacks.forEach { it.onIndexUpdate(value) }
        }
    private var mMode = PlaybackMode.ALL_SONGS
        set(value) {
            field = value
            callbacks.forEach { it.onModeUpdate(value) }
        }

    // Status
    private var mIsPlaying = false
        set(value) {
            field = value
            callbacks.forEach { it.onPlayingUpdate(value) }
        }

    private var mIsShuffling = false
        set(value) {
            field = value
            callbacks.forEach { it.onShuffleUpdate(value) }
        }
    private var mLoopMode = LoopMode.NONE
        set(value) {
            field = value
            callbacks.forEach { it.onLoopUpdate(value) }
        }
    private var mIsInUserQueue = false
        set(value) {
            field = value
            callbacks.forEach { it.onInUserQueueUpdate(value) }
        }
    private var mIsRestored = false
    private var mHasPlayed = false

    /** The currently playing song. Null if there isn't one */
    val song: Song? get() = mSong
    /** The parent the queue is based on, null if all_songs */
    val parent: Parent? get() = mParent
    /** The current playback progress */
    val position: Long get() = mPosition
    /** The current queue determined by [parent] and [mode] */
    val queue: List<Song> get() = mQueue
    /** The queue created by the user. */
    val userQueue: List<Song> get() = mUserQueue
    /** The current index of the queue */
    val index: Int get() = mIndex
    /** The current [PlaybackMode] */
    val mode: PlaybackMode get() = mMode
    /** Whether playback is paused or not */
    val isPlaying: Boolean get() = mIsPlaying
    /** Whether the queue is shuffled */
    val isShuffling: Boolean get() = mIsShuffling
    /** The current [LoopMode] */
    val loopMode: LoopMode get() = mLoopMode
    /** Whether this instance has already been restored */
    val isRestored: Boolean get() = mIsRestored
    /** Whether playback has begun in this instance during **PlaybackService's Lifecycle.** */
    val hasPlayed: Boolean get() = mHasPlayed

    private val settingsManager = SettingsManager.getInstance()
    private val musicStore = MusicStore.getInstance()

    // --- CALLBACKS ---

    private val callbacks = mutableListOf<Callback>()

    /**
     * Add a [PlaybackStateManager.Callback] to this instance.
     * Make sure to remove the callback with [removeCallback] when done.
     */
    fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    /**
     * Remove a [PlaybackStateManager.Callback] bound to this instance.
     */
    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    // --- PLAYING FUNCTIONS ---

    /**
     * Play a [song].
     * @param mode The [PlaybackMode] to construct the queue off of.
     */
    fun playSong(song: Song, mode: PlaybackMode) {
        logD("Updating song to ${song.name} and mode to $mode")

        when (mode) {
            PlaybackMode.ALL_SONGS -> {
                mParent = null
                mQueue = musicStore.songs.toMutableList()
            }

            PlaybackMode.IN_GENRE -> {
                val genre = song.genre

                // Dont do this if the genre is null
                if (genre != null) {
                    mParent = genre
                    mQueue = genre.songs.toMutableList()
                } else {
                    playSong(song, PlaybackMode.ALL_SONGS)

                    return
                }
            }

            PlaybackMode.IN_ARTIST -> {
                mParent = song.album.artist
                mQueue = song.album.artist.songs.toMutableList()
            }

            PlaybackMode.IN_ALBUM -> {
                mParent = song.album
                mQueue = song.album.songs.toMutableList()
            }
        }

        mMode = mode

        updatePlayback(song)
        // Keep shuffle on, if enabled
        setShuffling(settingsManager.keepShuffle && mIsShuffling, keepSong = true)
    }

    /**
     * Play a [parent], such as an artist or album.
     * @param shuffled Whether the queue is shuffled or not
     */
    fun playParent(parent: Parent, shuffled: Boolean) {
        logD("Playing ${parent.name}")

        mParent = parent
        mIndex = 0

        when (parent) {
            is Album -> {
                mQueue = parent.songs.toMutableList()
                mMode = PlaybackMode.IN_ALBUM
            }

            is Artist -> {
                mQueue = parent.songs.toMutableList()
                mMode = PlaybackMode.IN_ARTIST
            }

            is Genre -> {
                mQueue = parent.songs.toMutableList()
                mMode = PlaybackMode.IN_GENRE
            }
        }

        setShuffling(shuffled, keepSong = false)
        updatePlayback(mQueue[0])
    }

    /**
     * Shuffle all songs.
     */
    fun shuffleAll() {
        mMode = PlaybackMode.ALL_SONGS
        mQueue = musicStore.songs.toMutableList()
        mParent = null

        setShuffling(true, keepSong = false)
        updatePlayback(mQueue[0])
    }

    /**
     * Update the playback to a new [song], doing all the required logic.
     */
    private fun updatePlayback(song: Song, shouldPlay: Boolean = true) {
        mIsInUserQueue = false

        mSong = song
        mPosition = 0

        setPlaying(shouldPlay)
    }

    // --- QUEUE FUNCTIONS ---

    /**
     * Go to the next song, along with doing all the checks that entails.
     */
    fun next() {
        // If there's anything in the user queue, go to the first song in there instead
        // of incrementing the index.
        if (mUserQueue.isNotEmpty()) {
            updatePlayback(mUserQueue[0])
            mUserQueue.removeAt(0)

            // Mark that the playback state is currently in the user queue, for later.
            mIsInUserQueue = true

            forceUserQueueUpdate()
        } else {
            // Increment the index, if it cannot be incremented any further, then
            // loop and pause/resume playback depending on the setting
            if (mIndex < mQueue.lastIndex) {
                mIndex = mIndex.inc()
                updatePlayback(mQueue[mIndex])
            } else {
                mIndex = 0
                updatePlayback(mQueue[mIndex], shouldPlay = mLoopMode == LoopMode.ALL)
            }

            forceQueueUpdate()
        }
    }

    /**
     * Go to the previous song, doing any checks that are needed.
     */
    fun prev() {
        // If enabled, rewind before skipping back if the position is past 3 seconds [3000ms]
        if (settingsManager.rewindWithPrev && mPosition >= REWIND_THRESHOLD) {
            rewind()
        } else {
            // Only decrement the index if there's a song to move back to AND if we are not exiting
            // the user queue.
            if (mIndex > 0 && !mIsInUserQueue) {
                mIndex = mIndex.dec()
            }

            updatePlayback(mQueue[mIndex])
            forceQueueUpdate()
        }
    }

    // --- QUEUE EDITING FUNCTIONS ---

    /**
     * Remove a queue item at [index]. Will ignore invalid indexes.
     */
    fun removeQueueItem(index: Int): Boolean {
        logD("Removing item ${mQueue[index].name}.")

        if (index > mQueue.size || index < 0) {
            logE("Index is out of bounds, did not remove queue item.")

            return false
        }

        mQueue.removeAt(index)

        forceQueueUpdate()

        return true
    }

    /**
     * Move a queue item at [from] to a position at [to]. Will ignore invalid indexes.
     */
    fun moveQueueItems(from: Int, to: Int): Boolean {
        if (from > mQueue.size || from < 0 || to > mQueue.size || to < 0) {
            logE("Indices were out of bounds, did not move queue item")

            return false
        }

        val item = mQueue.removeAt(from)
        mQueue.add(to, item)

        forceQueueUpdate()

        return true
    }

    /**
     * Add a [song] to the user queue.
     */
    fun addToUserQueue(song: Song) {
        mUserQueue.add(song)

        forceUserQueueUpdate()
    }

    /**
     * Add a list of [songs] to the user queue.
     */
    fun addToUserQueue(songs: List<Song>) {
        mUserQueue.addAll(songs)

        forceUserQueueUpdate()
    }

    /**
     * Remove a USER queue item at [index]. Will ignore invalid indexes.
     */
    fun removeUserQueueItem(index: Int) {
        logD("Removing item ${mUserQueue[index].name}.")

        if (index > mUserQueue.size || index < 0) {
            logE("Index is out of bounds, did not remove user queue item.")

            return
        }

        mUserQueue.removeAt(index)

        forceUserQueueUpdate()
    }

    /**
     * Move a USER queue item at [from] to a position at [to]. Will ignore invalid indexes.
     */
    fun moveUserQueueItems(from: Int, to: Int) {
        if (from > mUserQueue.size || from < 0 || to > mUserQueue.size || to < 0) {
            logE("Indices were out of bounds, did not move user queue item")

            return
        }

        val item = mUserQueue.removeAt(from)
        mUserQueue.add(to, item)

        forceUserQueueUpdate()
    }

    /**
     * Clear the user queue. Forces a user queue update.
     */
    fun clearUserQueue() {
        mUserQueue.clear()

        forceUserQueueUpdate()
    }

    /**
     * Force any callbacks to receive a queue update.
     */
    private fun forceQueueUpdate() {
        mQueue = mQueue
    }

    /**
     * Force any callbacks to recieve a user queue update.
     */
    private fun forceUserQueueUpdate() {
        mUserQueue = mUserQueue
    }

    // --- SHUFFLE FUNCTIONS ---

    /**
     * Set whether this instance is [shuffled]. Updates the queue accordingly
     * @param keepSong Whether the current song should be kept as the queue is shuffled/unshuffled
     */
    fun setShuffling(shuffled: Boolean, keepSong: Boolean) {
        mIsShuffling = shuffled

        if (mIsShuffling) {
            genShuffle(keepSong, mIsInUserQueue)
        } else {
            resetShuffle(keepSong, mIsInUserQueue)
        }
    }

    /**
     * Generate a new shuffled queue.
     * @param keepSong Whether the current song should be kept as the queue is shuffled
     * @param useLastSong Whether to use the last song in the queue instead of the current one
     */
    private fun genShuffle(keepSong: Boolean, useLastSong: Boolean) {
        val lastSong = if (useLastSong) mQueue[0] else mSong

        logD("Shuffling queue")

        mQueue.shuffle()
        mIndex = 0

        // If specified, make the current song the first member of the queue.
        if (keepSong) {
            moveQueueItems(mQueue.indexOf(lastSong), 0)
        } else {
            // Otherwise, just start from the zeroth position in the queue.
            mSong = mQueue[0]
        }

        forceQueueUpdate()
    }

    /**
     * Reset the queue to its normal, ordered state.
     * @param keepSong Whether the current song should be kept as the queue is unshuffled
     * @param useLastSong Whether to use the previous song for the index calculations.
     */
    private fun resetShuffle(keepSong: Boolean, useLastSong: Boolean) {
        val lastSong = if (useLastSong) mQueue[mIndex] else mSong

        mQueue = when (mMode) {
            PlaybackMode.IN_ARTIST -> orderSongsInArtist(mParent as Artist)
            PlaybackMode.IN_ALBUM -> orderSongsInAlbum(mParent as Album)
            PlaybackMode.IN_GENRE -> orderSongsInGenre(mParent as Genre)
            PlaybackMode.ALL_SONGS -> musicStore.songs.toMutableList()
        }

        if (keepSong) {
            mIndex = mQueue.indexOf(lastSong)
        }

        forceQueueUpdate()
    }

    // --- STATE FUNCTIONS ---

    /**
     * Set whether this instance is currently [playing].
     */
    fun setPlaying(playing: Boolean) {
        if (mIsPlaying != playing) {
            if (playing) {
                mHasPlayed = true
            }

            mIsPlaying = playing
        }
    }

    /**
     * Update the current [position]. Will not notify listeners of a seek event.
     * @param position The new position in millis.
     * @see seekTo
     */
    fun setPosition(position: Long) {
        mSong?.let { song ->
            // Don't accept any bugged positions that are over the duration of the song.
            if (position <= song.duration) {
                mPosition = position
            }
        }
    }

    /**
     * **Seek** to a [position], this calls [PlaybackStateManager.Callback.onSeek] to notify
     * elements that rely on that.
     * @param position The position to seek to in millis.
     */
    fun seekTo(position: Long) {
        mPosition = position

        callbacks.forEach { it.onSeek(position) }
    }

    /**
     * Rewind to the beginning of a song.
     */
    fun rewind() {
        seekTo(0)
        setPlaying(true)
    }

    /**
     * Set the [LoopMode] to [mode].
     */
    fun setLoopMode(mode: LoopMode) {
        mLoopMode = mode
    }

    /**
     * Mark whether this instance has played or not
     */
    fun setHasPlayed(hasPlayed: Boolean) {
        mHasPlayed = hasPlayed
    }

    /**
     * Mark this instance as restored.
     */
    fun markRestored() {
        mIsRestored = true
    }

    // --- PERSISTENCE FUNCTIONS ---

    /**
     * Save the current state to the database.
     * @param context [Context] required
     */
    suspend fun saveStateToDatabase(context: Context) {
        logD("Saving state to DB.")

        // Pack the entire state and save it to the database.
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()

            val database = PlaybackStateDatabase.getInstance(context)

            database.writeState(packToPlaybackState())
            database.writeQueue(packQueue())

            this@PlaybackStateManager.logD(
                "Save finished in ${System.currentTimeMillis() - start}ms"
            )
        }
    }

    /**
     * Restore the state from the database
     * @param context [Context] required.
     */
    suspend fun restoreFromDatabase(context: Context) {
        logD("Getting state from DB.")

        val start: Long
        val playbackState: PlaybackState?
        val queueItems: List<QueueItem>

        withContext(Dispatchers.IO) {
            start = System.currentTimeMillis()

            val database = PlaybackStateDatabase.getInstance(context)

            playbackState = database.readState()
            queueItems = database.readQueue()
        }

        // Get off the IO coroutine since it will cause LiveData updates to throw an exception

        if (playbackState != null) {
            logD("Found playback state $playbackState with queue size ${queueItems.size}")

            unpackFromPlaybackState(playbackState)
            unpackQueue(queueItems)
            doParentSanityCheck()
        }

        logD("Restore finished in ${System.currentTimeMillis() - start}ms")

        markRestored()
    }

    /**
     * Pack the current state into a [PlaybackState] to be saved.
     * @return A [PlaybackState] reflecting the current state.
     */
    private fun packToPlaybackState(): PlaybackState {
        return PlaybackState(
            songHash = mSong?.hash ?: Int.MIN_VALUE,
            position = mPosition,
            parentHash = mParent?.hash ?: Int.MIN_VALUE,
            index = mIndex,
            mode = mMode.toInt(),
            isShuffling = mIsShuffling,
            loopMode = mLoopMode.toInt(),
            inUserQueue = mIsInUserQueue
        )
    }

    /**
     * Unpack a [playbackState] into this instance.
     */
    private fun unpackFromPlaybackState(playbackState: PlaybackState) {
        // Turn the simplified information from PlaybackState into usable data.

        // Do queue setup first
        mMode = PlaybackMode.fromInt(playbackState.mode) ?: PlaybackMode.ALL_SONGS
        mParent = findParent(playbackState.parentHash, mMode)
        mIndex = playbackState.index

        // Then set up the current state
        mSong = musicStore.songs.find { it.hash == playbackState.songHash }
        mLoopMode = LoopMode.fromInt(playbackState.loopMode) ?: LoopMode.NONE
        mIsShuffling = playbackState.isShuffling
        mIsInUserQueue = playbackState.inUserQueue

        seekTo(playbackState.position)
    }

    /**
     * Pack the queue into a list of [QueueItem]s to be saved.
     * @return A list of packed queue items.
     */
    private fun packQueue(): List<QueueItem> {
        val unified = mutableListOf<QueueItem>()

        var queueItemId = 0L

        mUserQueue.forEach { song ->
            unified.add(QueueItem(queueItemId, song.hash, song.album.hash, true))
            queueItemId++
        }

        mQueue.forEach { song ->
            unified.add(QueueItem(queueItemId, song.hash, song.album.hash, false))
            queueItemId++
        }

        return unified
    }

    /**
     * Unpack a list of queue items into a queue & user queue.
     * @param queueItems The list of [QueueItem]s to unpack.
     */
    private fun unpackQueue(queueItems: List<QueueItem>) {
        for (item in queueItems) {
            musicStore.findSongFast(item.songHash, item.albumHash)?.let { song ->
                if (item.isUserQueue) {
                    mUserQueue.add(song)
                } else {
                    mQueue.add(song)
                }
            }
        }

        // When done, get a more accurate index to prevent issues with queue songs that were saved
        // to the db but are now deleted when the restore occurred.
        // Not done if in user queue because that could result in a bad index being created.
        if (!mIsInUserQueue) {
            mSong?.let { song ->
                val index = mQueue.indexOf(song)
                mIndex = if (index != -1) index else mIndex
            }
        }

        forceQueueUpdate()
        forceUserQueueUpdate()
    }

    /**
     * Get a [Parent] from music store given a [hash] and PlaybackMode [mode].
     */
    private fun findParent(hash: Int, mode: PlaybackMode): Parent? {
        return when (mode) {
            PlaybackMode.IN_GENRE -> musicStore.genres.find { it.hash == hash }
            PlaybackMode.IN_ARTIST -> musicStore.artists.find { it.hash == hash }
            PlaybackMode.IN_ALBUM -> musicStore.albums.find { it.hash == hash }
            PlaybackMode.ALL_SONGS -> null
        }
    }

    /**
     * Do the sanity check to make sure the parent was not lost in the restore process.
     */
    private fun doParentSanityCheck() {
        // Check if the parent was lost while in the DB.
        if (mSong != null && mParent == null && mMode != PlaybackMode.ALL_SONGS) {
            logD("Parent lost, attempting restore.")

            mParent = when (mMode) {
                PlaybackMode.IN_ALBUM -> mQueue.firstOrNull()?.album
                PlaybackMode.IN_ARTIST -> mQueue.firstOrNull()?.album?.artist
                PlaybackMode.IN_GENRE -> mQueue.firstOrNull()?.genre
                PlaybackMode.ALL_SONGS -> null
            }
        }
    }

    // --- ORDERING FUNCTIONS ---

    /**
     * Create an ordered queue based on an [Album].
     */
    private fun orderSongsInAlbum(album: Album): MutableList<Song> {
        return settingsManager.albumSortMode.getSortedSongList(album.songs).toMutableList()
    }

    /**
     * Create an ordered queue based on an [Artist].
     */
    private fun orderSongsInArtist(artist: Artist): MutableList<Song> {
        return settingsManager.artistSortMode.getSortedArtistSongList(artist.songs).toMutableList()
    }

    /**
     * Create an ordered queue based on a [Genre].
     */
    private fun orderSongsInGenre(genre: Genre): MutableList<Song> {
        return settingsManager.genreSortMode.getSortedSongList(genre.songs).toMutableList()
    }

    /**
     * The interface for receiving updates from [PlaybackStateManager].
     * Add the callback to [PlaybackStateManager] using [addCallback],
     * remove them on destruction with [removeCallback].
     */
    interface Callback {
        fun onSongUpdate(song: Song?) {}
        fun onParentUpdate(parent: Parent?) {}
        fun onPositionUpdate(position: Long) {}
        fun onQueueUpdate(queue: List<Song>) {}
        fun onUserQueueUpdate(userQueue: List<Song>) {}
        fun onModeUpdate(mode: PlaybackMode) {}
        fun onIndexUpdate(index: Int) {}
        fun onPlayingUpdate(isPlaying: Boolean) {}
        fun onShuffleUpdate(isShuffling: Boolean) {}
        fun onLoopUpdate(loopMode: LoopMode) {}
        fun onSeek(position: Long) {}
        fun onInUserQueueUpdate(isInUserQueue: Boolean) {}
    }

    companion object {
        private const val REWIND_THRESHOLD = 3000L

        @Volatile
        private var INSTANCE: PlaybackStateManager? = null

        /**
         * Get/Instantiate the single instance of [PlaybackStateManager].
         */
        fun getInstance(): PlaybackStateManager {
            val currentInstance = INSTANCE

            if (currentInstance != null) {
                return currentInstance
            }

            synchronized(this) {
                val newInstance = PlaybackStateManager()
                INSTANCE = newInstance
                return newInstance
            }
        }
    }
}
