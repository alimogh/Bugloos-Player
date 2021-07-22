package org.hfathi.bugloos.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.databinding.BindingAdapter
import org.hfathi.bugloos.R
import org.hfathi.bugloos.ui.getPlural

/**
 * A complete array of all the hardcoded genre values for ID3 <v3, contains standard genres and
 * winamp extensions.
 */
private val ID3_GENRES = arrayOf(
    // ID3 Standard
    "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz",
    "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno",
    "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno",
    "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental",
    "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk",
    "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave",
    "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy",
    "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American",
    "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal",
    "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock",

    // Winamp Extensions
    "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival",
    "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock",
    "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour",
    "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus",
    "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad",
    "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella",
    "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie",
    "Britpop", "Negerpunk", "Polsk Punk", "Beat", "Christian Gangsta", "Heavy Metal", "Black Metal",
    "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal",
    "Anime", "JPop", "Synthpop",

    // Winamp 5.6+ extensions, used by EasyTAG and friends
    "Abstract", "Art Rock", "Baroque", "Bhangra", "Big Beat", "Breakbeat", "Chillout", "Downtempo",
    "Dub", "EBM", "Eclectic", "Electro", "Electroclash", "Emo", "Experimental", "Garage", "Global",
    "IDM", "Illbient", "Industro-Goth", "Jam Band", "Krautrock", "Leftfield", "Lounge", "Math Rock", // S I X T Y   F I V E
    "New Romantic", "Nu-Breakz", "Post-Punk", "Post-Rock", "Psytrance", "Shoegaze", "Space Rock",
    "Trop Rock", "World Music", "Neoclassical", "Audiobook", "Audio Theatre", "Neue Deutsche Welle",
    "Podcast", "Indie Rock", "G-Funk", "Dubstep", "Garage Rock", "Psybient"
)

// --- EXTENSION FUNCTIONS ---

/**
 * Convert legacy int-based ID3 genres to their human-readable genre
 * @return The named genre for this legacy genre, null if there is no need to parse it
 * or if the genre is invalid.
 */
fun String.getGenreNameCompat(): String? {
    if (isDigitsOnly()) {
        // ID3 v1, just parse as an integer
        return ID3_GENRES.getOrNull(toInt())
    }

    if (startsWith('(') && endsWith(')')) {
        // ID3 v2+, parse out the parentheses and get the integer
        // Any genres formatted as "(CHARS)" will be ignored.
        val genreInt = substring(1 until lastIndex).toIntOrNull()

        if (genreInt != null) {
            return ID3_GENRES.getOrNull(genreInt)
        }
    }

    // Current name is fine.
    return null
}

/**
 * Convert an id to its corresponding URI
 */
fun Long.toURI(): Uri {
    return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this)
}

/**
 * Get the URI for an album's cover art, corresponds to MediaStore.
 */
fun Long.toAlbumArtURI(): Uri {
    return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), this)
}

/**
 * Convert a [Long] of seconds into a string duration.
 */
fun Long.toDuration(): String {
    var durationString = DateUtils.formatElapsedTime(this)

    // If the duration begins with a excess zero [e.g 01:42], then cut it off.
    if (durationString[0] == '0') {
        durationString = durationString.slice(1 until durationString.length)
    }

    return durationString
}

/**
 * Convert an integer to its formatted year.
 */
fun Int.toYear(context: Context): String {
    return if (this > 0) {
        toString()
    } else {
        context.getString(R.string.placeholder_no_date)
    }
}

// --- BINDING ADAPTERS ---

/**
 * Bind the most prominent artist genre
 */
@BindingAdapter("artistGenre")
fun TextView.bindArtistGenre(artist: Artist) {
    text = artist.genre?.resolvedName ?: context.getString(R.string.placeholder_genre)
}

/**
 * Bind the album + song counts for an artist
 */
@BindingAdapter("artistCounts")
fun TextView.bindArtistCounts(artist: Artist) {
    val albums = context.getPlural(R.plurals.format_album_count, artist.albums.size)
    val songs = context.getPlural(R.plurals.format_song_count, artist.songs.size)

    text = context.getString(R.string.format_double_counts, albums, songs)
}

/**
 * Get all album information, used on [org.hfathi.bugloos.detail.AlbumDetailFragment]
 */
@BindingAdapter("albumDetails")
fun TextView.bindAllAlbumDetails(album: Album) {
    text = context.getString(
        R.string.format_double_info,
        album.year.toYear(context),
        context.getPlural(R.plurals.format_song_count, album.songs.size),
        album.totalDuration
    )
}

/**
 * Get basic information about an album, used on album ViewHolders
 */
@BindingAdapter("albumInfo")
fun TextView.bindAlbumInfo(album: Album) {
    text = context.getString(
        R.string.format_info,
        album.artist.name,
        context.getPlural(R.plurals.format_song_count, album.songs.size),
    )
}

/**
 * Bind the year for an album.
 */
@BindingAdapter("albumYear")
fun TextView.bindAlbumYear(album: Album) {
    text = album.year.toYear(context)
}
