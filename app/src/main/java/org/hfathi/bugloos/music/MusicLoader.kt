package org.hfathi.bugloos.music

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Genres
import android.provider.MediaStore.Audio.Media
import androidx.core.database.getStringOrNull
import org.hfathi.bugloos.R
import org.hfathi.bugloos.database.BlacklistDatabase
import org.hfathi.bugloos.logD

/**
 * Class that loads/constructs [Genre]s, [Artist]s, [Album]s, and [Song] objects from the filesystem
 * @author hamid fathi
 *
 * FIXME: Here's a catalog of problems that I already know about with this abomination
 *  - All loading is done at startup [Not efficent for large libraries, would require massive arch retooling to fix]
 *  - Does not support the album artist tag [Nothing I can do that doesn't involve rolling my own loader]
 *  - Genre system is a bottleneck [See Above]
 *  Blame MediaStore, loading anything on this platform is a nightmare.
 */
class MusicLoader(private val context: Context) {
    var genres = mutableListOf<Genre>()
    var albums = mutableListOf<Album>()
    var artists = mutableListOf<Artist>()
    var songs = mutableListOf<Song>()

    private val resolver = context.contentResolver

    private var selector = "${Media.IS_MUSIC}=1"
    private var args = arrayOf<String>()

    /**
     * Begin the loading process.
     * Resulting models are pushed to [genres], [artists], [albums], and [songs].
     */
    fun load() {
        buildSelector()

        loadGenres()
        loadAlbums()
        loadSongs()

        linkAlbums()
        buildArtists()
        linkGenres()
    }

    @Suppress("DEPRECATION")
    private fun buildSelector() {
        // TODO: Upgrade this to be compatible with Android Q.
        val blacklistDatabase = BlacklistDatabase.getInstance(context)

        val paths = blacklistDatabase.readPaths()

        for (path in paths) {
            selector += " AND ${Media.DATA} NOT LIKE ?"
            args += "$path%" // Append % so that the selector properly detects children
        }
    }

    private fun loadGenres() {
        logD("Starting genre search...")

        // First, get a cursor for every genre in the android system
        val genreCursor = resolver.query(
            Genres.EXTERNAL_CONTENT_URI,
            arrayOf(
                Genres._ID, // 0
                Genres.NAME // 1
            ),
            null, null,
            Genres.DEFAULT_SORT_ORDER
        )

        // And then process those into Genre objects
        genreCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Genres._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(Genres.NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getStringOrNull(nameIndex) ?: continue // No non-broken genre would be missing a name

                genres.add(Genre(id, name))
            }
        }

        logD("Genre search finished with ${genres.size} genres found.")
    }

    private fun loadAlbums() {
        logD("Starting album search...")

        val albumCursor = resolver.query(
            Albums.EXTERNAL_CONTENT_URI,
            arrayOf(
                Albums._ID, // 0
                Albums.ALBUM, // 1
                Albums.ARTIST, // 2
                Albums.LAST_YEAR, // 4
            ),
            null, null,
            Albums.DEFAULT_SORT_ORDER
        )

        val albumPlaceholder = context.getString(R.string.placeholder_album)
        val artistPlaceholder = context.getString(R.string.placeholder_artist)

        albumCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Albums._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(Albums.ALBUM)
            val artistNameIndex = cursor.getColumnIndexOrThrow(Albums.ARTIST)
            val yearIndex = cursor.getColumnIndexOrThrow(Albums.LAST_YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: albumPlaceholder
                var artistName = cursor.getString(artistNameIndex) ?: artistPlaceholder
                val year = cursor.getInt(yearIndex)
                val coverUri = id.toAlbumArtURI()

                // Correct any artist names to a nicer "Unknown Artist" label
                if (artistName == MediaStore.UNKNOWN_STRING) {
                    artistName = artistPlaceholder
                }

                albums.add(Album(id, name, artistName, coverUri, year))
            }
        }

        albums = albums.distinctBy {
            it.name to it.artistName to it.year
        }.toMutableList()

        logD("Album search finished with ${albums.size} albums found")
    }

    @SuppressLint("InlinedApi")
    private fun loadSongs() {
        logD("Starting song search...")

        val songCursor = resolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                Media._ID, // 0
                Media.DISPLAY_NAME, // 1
                Media.TITLE, // 2
                Media.ALBUM_ID, // 3
                Media.TRACK, // 4
                Media.DURATION, // 5
            ),
            selector, args,
            Media.DEFAULT_SORT_ORDER
        )

        songCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(Media.TITLE)
            val fileIndex = cursor.getColumnIndexOrThrow(Media.DISPLAY_NAME)
            val albumIndex = cursor.getColumnIndexOrThrow(Media.ALBUM_ID)
            val trackIndex = cursor.getColumnIndexOrThrow(Media.TRACK)
            val durationIndex = cursor.getColumnIndexOrThrow(Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val fileName = cursor.getString(fileIndex)
                val title = cursor.getString(titleIndex) ?: fileName
                val albumId = cursor.getLong(albumIndex)
                val track = cursor.getInt(trackIndex)
                val duration = cursor.getLong(durationIndex)

                songs.add(Song(id, title, fileName, albumId, track, duration))
            }
        }

        songs = songs.distinctBy {
            it.name to it.albumId to it.track to it.duration
        }.toMutableList()

        logD("Song search finished with ${songs.size} found")
    }

    private fun linkAlbums() {
        logD("Linking albums")

        // Group up songs by their album ids and then link them with their albums
        val songsByAlbum = songs.groupBy { it.albumId }
        val unknownAlbum = Album(
            id = -1,
            name = context.getString(R.string.placeholder_album),
            artistName = context.getString(R.string.placeholder_artist),
            coverUri = Uri.EMPTY,
            year = 0
        )

        songsByAlbum.forEach { entry ->
            (albums.find { it.id == entry.key } ?: unknownAlbum).linkSongs(entry.value)
        }

        albums.removeAll { it.songs.isEmpty() }

        // If something goes horribly wrong and somehow songs are still not linked up by the
        // album id, just throw them into an unknown album.
        if (unknownAlbum.songs.isNotEmpty()) {
            albums.add(unknownAlbum)
        }
    }

    private fun buildArtists() {
        logD("Linking artists")

        // Group albums up by their artist name, should not result in any null-artist issues
        val albumsByArtist = albums.groupBy { it.artistName }

        albumsByArtist.forEach { entry ->
            artists.add(
                // IDs are incremented from the minimum int value so that they remain unique.
                Artist(
                    id = (Int.MIN_VALUE + artists.size).toLong(),
                    name = entry.key,
                    albums = entry.value
                )
            )
        }

        logD("Albums successfully linked into ${artists.size} artists")
    }

    private fun linkGenres() {
        logD("Linking genres")

        genres.forEach { genre ->
            val songCursor = resolver.query(
                Genres.Members.getContentUri("external", genre.id),
                arrayOf(Genres.Members._ID),
                null, null, null // Dont even bother selecting here as useless iters are less expensive than IO
            )

            songCursor?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Genres.Members._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)

                    songs.find { it.id == id }?.let { song ->
                        genre.linkSong(song)
                    }
                }
            }
        }

        // Any songs without genres will be thrown into an unknown genre
        val songsWithoutGenres = songs.filter { it.genre == null }

        if (songsWithoutGenres.isNotEmpty()) {
            val unknownGenre = Genre(
                id = -2,
                name = context.getString(R.string.placeholder_genre)
            )

            songsWithoutGenres.forEach { song ->
                unknownGenre.linkSong(song)
            }

            genres.add(unknownGenre)
        }

        genres.removeAll { it.songs.isEmpty() }
    }
}
