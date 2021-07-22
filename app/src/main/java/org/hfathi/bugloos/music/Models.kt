package org.hfathi.bugloos.music

import android.net.Uri

// --- MUSIC MODELS ---

/**
 * The base data object for all music.
 * @property id The ID that is assigned to this object
 * @property name The name of this object (Such as a song title)
 */
sealed class BaseModel {
    abstract val id: Long
    abstract val name: String
}

/**
 * [BaseModel] variant that denotes that this object is a parent of other data objects, such
 * as an [Album] or [Artist]
 * @property hash A versatile, unique(ish) hash used for databases
 * @property displayName Name that handles the usage of [Genre.resolvedName]
 * and the normal [BaseModel.name]
 */
sealed class Parent : BaseModel() {
    abstract val hash: Int

    val displayName: String get() = if (this is Genre) {
        resolvedName
    } else {
        name
    }
}

/**
 * The data object for a song. Inherits [BaseModel].
 * @property fileName The raw filename for this track
 * @property albumId  The Song's Album ID.
 * Never use this outside of when attaching a song to its album.
 * @property track    The Song's Track number
 * @property duration The duration of the song, in millis.
 * @property album    The Song's parent album. Use this instead of [albumId].
 * @property genre    The Song's [Genre].
 * These are not ensured to be linked due to possible quirks in the genre loading system.
 * @property seconds  The Song's duration in seconds
 * @property formattedDuration The Song's duration as a duration string.
 * @property hash     A versatile, unique(ish) hash used for databases
 */
data class Song(
    override val id: Long,
    override val name: String,
    val fileName: String,
    val albumId: Long,
    val track: Int,
    val duration: Long
) : BaseModel() {
    private var mAlbum: Album? = null
    private var mGenre: Genre? = null

    val genre: Genre? get() = mGenre
    val album: Album get() = requireNotNull(mAlbum)

    val seconds = duration / 1000
    val formattedDuration = seconds.toDuration()

    val hash = songHash()

    fun linkAlbum(album: Album) {
        if (mAlbum == null) {
            mAlbum = album
        }
    }

    fun linkGenre(genre: Genre) {
        if (mGenre == null) {
            mGenre = genre
        }
    }

    private fun songHash(): Int {
        var result = name.hashCode()
        result = 31 * result + track
        result = 31 * result + duration.hashCode()
        return result
    }
}

/**
 * The data object for an album. Inherits [Parent].
 * @property artistName    The name of the parent artist. Do not use this outside of creating the artist from albums
 * @property coverUri      The [Uri] for the album's cover. **Load this using Coil.**
 * @property year          The year this album was released. 0 if there is none in the metadata.
 * @property artist        The Album's parent [Artist]. use this instead of [artistName]
 * @property songs         The Album's child [Song]s.
 * @property totalDuration The combined duration of all of the album's child songs, formatted.
 */
data class Album(
    override val id: Long,
    override val name: String,
    val artistName: String,
    val coverUri: Uri,
    val year: Int
) : Parent() {
    private var mArtist: Artist? = null
    val artist: Artist get() = requireNotNull(mArtist)

    private val mSongs = mutableListOf<Song>()
    val songs: List<Song> get() = mSongs

    val totalDuration: String get() =
        songs.sumOf { it.seconds }.toDuration()

    override val hash = albumHash()

    fun linkArtist(artist: Artist) {
        mArtist = artist
    }

    fun linkSongs(songs: List<Song>) {
        for (song in songs) {
            song.linkAlbum(this)
            mSongs.add(song)
        }
    }

    private fun albumHash(): Int {
        var result = name.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + year
        return result
    }
}

/**
 * The data object for an artist. Inherits [Parent]
 * @property albums The list of all [Album]s in this artist
 * @property genre  The most prominent genre for this artist
 * @property songs  The list of all [Song]s in this artist
 */
data class Artist(
    override val id: Long,
    override val name: String,
    val albums: List<Album>
) : Parent() {
    val genre: Genre? by lazy {
        // Get the genre that corresponds to the most songs in this artist, which would be
        // the most "Prominent" genre.
        songs.groupBy { it.genre }.entries.maxByOrNull { it.value.size }?.key
    }

    val songs: List<Song> by lazy {
        albums.flatMap { it.songs }
    }

    override val hash = name.hashCode()

    init {
        albums.forEach { album ->
            album.linkArtist(this)
        }
    }
}

/**
 * The data object for a genre. Inherits [Parent]
 * @property songs   The list of all [Song]s in this genre.
 * @property resolvedName A name that has been resolved from its int-genre form to its named form.
 */
data class Genre(
    override val id: Long,
    override val name: String,
) : Parent() {
    private val mSongs = mutableListOf<Song>()
    val songs: List<Song> get() = mSongs

    val resolvedName =
        name.getGenreNameCompat() ?: name

    val totalDuration: String get() =
        songs.sumOf { it.seconds }.toDuration()

    override val hash = name.hashCode()

    fun linkSong(song: Song) {
        mSongs.add(song)
        song.linkGenre(this)
    }
}

/**
 * A data object used solely for the "Header" UI element. Inherits [BaseModel].
 * @param isAction Whether this header corresponds to an action or not
 */
data class Header(
    override val id: Long,
    override val name: String,
    val isAction: Boolean = false
) : BaseModel()
