package org.hfathi.bugloos.coil

import android.content.Context
import android.media.MediaMetadataRetriever
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import okio.buffer
import okio.source
import org.hfathi.bugloos.music.Album
import org.hfathi.bugloos.music.toURI
import org.hfathi.bugloos.settings.SettingsManager
import java.io.ByteArrayInputStream

/**
 * Fetcher that returns the album art for a given [Album]. Handles settings on whether to use
 * quality covers or not.
 * @author hamid fathi
 */
class AlbumArtFetcher(private val context: Context) : Fetcher<Album> {
    private val settingsManager = SettingsManager.getInstance()

    override suspend fun fetch(
        pool: BitmapPool,
        data: Album,
        size: Size,
        options: Options
    ): FetchResult {
        return if (settingsManager.useQualityCovers) {
            loadQualityCovers(data)
        } else {
            loadMediaStoreCovers(data)
        }
    }

    private fun loadMediaStoreCovers(data: Album): SourceResult {
        val stream = context.contentResolver.openInputStream(data.coverUri)

        if (stream != null) {
            // Don't close the stream here as it will cause an error later from an attempted read.
            // This stream still seems to close itself at some point, so its fine.
            return SourceResult(
                source = stream.source().buffer(),
                mimeType = context.contentResolver.getType(data.coverUri),
                dataSource = DataSource.DISK
            )
        }

        error("No cover art for album ${data.name}")
    }

    private fun loadQualityCovers(data: Album): SourceResult {
        val extractor = MediaMetadataRetriever()

        extractor.use { ext ->
            val songUri = data.songs[0].id.toURI()
            ext.setDataSource(context, songUri)

            // Get the embedded picture from MediaMetadataRetriever, which will return a full
            // ByteArray of the cover without any compression artifacts.
            // If its null [a.k.a there is no embedded cover], than just ignore it and move on
            ext.embeddedPicture?.let { coverBytes ->
                val stream = ByteArrayInputStream(coverBytes)

                return SourceResult(
                    source = stream.source().buffer(),
                    mimeType = context.contentResolver.getType(songUri),
                    dataSource = DataSource.DISK
                )
            }
        }

        // If we are here, the extractor likely failed so instead attempt to return the compressed
        // cover instead.
        return loadMediaStoreCovers(data)
    }

    override fun key(data: Album) = data.coverUri.toString()
}
