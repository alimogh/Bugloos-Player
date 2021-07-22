package org.hfathi.bugloos.recycler.viewholders

import android.content.Context
import android.view.View
import org.hfathi.bugloos.databinding.ItemAlbumBinding
import org.hfathi.bugloos.databinding.ItemArtistBinding
import org.hfathi.bugloos.databinding.ItemGenreBinding
import org.hfathi.bugloos.databinding.ItemHeaderBinding
import org.hfathi.bugloos.databinding.ItemSongBinding
import org.hfathi.bugloos.music.Album
import org.hfathi.bugloos.music.Artist
import org.hfathi.bugloos.music.Genre
import org.hfathi.bugloos.music.Header
import org.hfathi.bugloos.music.Song
import org.hfathi.bugloos.ui.inflater

/**
 * The Shared ViewHolder for a [Song]. Instantiation should be done with [from].
 */
class SongViewHolder private constructor(
    private val binding: ItemSongBinding,
    doOnClick: (data: Song) -> Unit,
    doOnLongClick: (view: View, data: Song) -> Unit
) : BaseViewHolder<Song>(binding, doOnClick, doOnLongClick) {

    override fun onBind(data: Song) {
        binding.song = data

        binding.songName.requestLayout()
        binding.songInfo.requestLayout()
    }

    companion object {
        const val ITEM_TYPE = 0xA000

        /**
         * Create an instance of [SongViewHolder]
         */
        fun from(
            context: Context,
            doOnClick: (data: Song) -> Unit,
            doOnLongClick: (view: View, data: Song) -> Unit
        ): SongViewHolder {
            return SongViewHolder(
                ItemSongBinding.inflate(context.inflater),
                doOnClick, doOnLongClick
            )
        }
    }
}

/**
 * The Shared ViewHolder for a [Album]. Instantiation should be done with [from].
 */
class AlbumViewHolder private constructor(
    private val binding: ItemAlbumBinding,
    doOnClick: (data: Album) -> Unit,
    doOnLongClick: (view: View, data: Album) -> Unit
) : BaseViewHolder<Album>(binding, doOnClick, doOnLongClick) {

    override fun onBind(data: Album) {
        binding.album = data
        binding.albumName.requestLayout()
    }

    companion object {
        const val ITEM_TYPE = 0xA001

        /**
         * Create an instance of [AlbumViewHolder]
         */
        fun from(
            context: Context,
            doOnClick: (data: Album) -> Unit,
            doOnLongClick: (view: View, data: Album) -> Unit
        ): AlbumViewHolder {
            return AlbumViewHolder(
                ItemAlbumBinding.inflate(context.inflater),
                doOnClick, doOnLongClick
            )
        }
    }
}

/**
 * The Shared ViewHolder for a [Artist]. Instantiation should be done with [from].
 */
class ArtistViewHolder private constructor(
    private val binding: ItemArtistBinding,
    doOnClick: (Artist) -> Unit,
    doOnLongClick: (view: View, data: Artist) -> Unit
) : BaseViewHolder<Artist>(binding, doOnClick, doOnLongClick) {

    override fun onBind(data: Artist) {
        binding.artist = data
        binding.artistName.requestLayout()
    }

    companion object {
        const val ITEM_TYPE = 0xA002

        /**
         * Create an instance of [ArtistViewHolder]
         */
        fun from(
            context: Context,
            doOnClick: (Artist) -> Unit,
            doOnLongClick: (view: View, data: Artist) -> Unit
        ): ArtistViewHolder {
            return ArtistViewHolder(
                ItemArtistBinding.inflate(context.inflater),
                doOnClick, doOnLongClick
            )
        }
    }
}

/**
 * The Shared ViewHolder for a [Genre]. Instantiation should be done with [from].
 */
class GenreViewHolder private constructor(
    private val binding: ItemGenreBinding,
    doOnClick: (Genre) -> Unit,
    doOnLongClick: (view: View, data: Genre) -> Unit
) : BaseViewHolder<Genre>(binding, doOnClick, doOnLongClick) {

    override fun onBind(data: Genre) {
        binding.genre = data
        binding.genreName.requestLayout()
    }

    companion object {
        const val ITEM_TYPE = 0xA003

        /**
         * Create an instance of [GenreViewHolder]
         */
        fun from(
            context: Context,
            doOnClick: (Genre) -> Unit,
            doOnLongClick: (view: View, data: Genre) -> Unit
        ): GenreViewHolder {
            return GenreViewHolder(
                ItemGenreBinding.inflate(context.inflater),
                doOnClick, doOnLongClick
            )
        }
    }
}

/**
 * The Shared ViewHolder for a [Header]. Instantiation should be done with [from]
 */
class HeaderViewHolder private constructor(
    private val binding: ItemHeaderBinding
) : BaseViewHolder<Header>(binding) {

    override fun onBind(data: Header) {
        binding.header = data
    }

    companion object {
        const val ITEM_TYPE = 0xA004

        /**
         * Create an instance of [HeaderViewHolder]
         */
        fun from(context: Context): HeaderViewHolder {
            return HeaderViewHolder(
                ItemHeaderBinding.inflate(context.inflater)
            )
        }
    }
}
