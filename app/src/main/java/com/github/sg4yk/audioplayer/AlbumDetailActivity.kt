package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.github.sg4yk.audioplayer.utils.PrefManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_album_detail.*
import kotlinx.android.synthetic.main.activity_album_detail.recyclerView
import kotlinx.android.synthetic.main.activity_album_detail.toolbar
import kotlinx.android.synthetic.main.activity_artist_detail.*
import kotlinx.android.synthetic.main.album_detail_audio_item.view.*
import kotlinx.coroutines.*

class AlbumDetailActivity : AppCompatActivity() {
    companion object {
        private var threshold: Int? = null
        private var statusBarPosThreshold: Int? = null

        const val STATUS_BAR_LUMINANCE_THRESHOLD = 100
        const val FAB_LUMINANCE_THRESHOLD = 127
        const val METADATA_TAG = "EXTRA_METADATA"
        const val IS_DARK_ALBUM_ART_TAG = "EXTRA_IS_DARK_ALBUM_ART"

        private const val IS_DARK_ALBUM = 0
        private const val IS_LIGHT_ALBUM = 1
        private const val NOT_SET = -1

        private val whiteFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.DST)

        fun start(context: Context, albumId: Long, card: View? = null, albumArt: Bitmap? = null) {
            GlobalScope.launch(Dispatchers.Main) {
                val albums = MediaHunter.getAllAlbums(context).filter { album -> album.id == albumId }
                if (albums.size == 1) {
                    start(context, albums[0], card, albumArt)
                }
            }
        }

        fun start(context: Context, album: Album, card: View? = null, albumArt: Bitmap? = null) {
            start(context, album.id, album.album, album.artist, card, albumArt)
        }

        private fun start(
            context: Context,
            albumId: Long,
            albumName: String?,
            artist: String?,
            card: View? = null,
            albumArt: Bitmap? = null
        ) {
            val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                putExtra(
                    METADATA_TAG,
                    arrayOf(
                        albumName ?: "Unknown album",
                        artist ?: "Unknown artist",
                        albumId.toString()
                    )
                )
            }
            if (albumArt != null) {
                // Determine whether should use light status bar
                Palette.from(albumArt).generate { palette ->
                    GlobalScope.launch {
                        val primaryColor = palette?.getDominantColor(Color.WHITE) ?: Color.WHITE
                        intent.putExtra(
                            IS_DARK_ALBUM_ART_TAG,
                            if (Generic.luminance(primaryColor) < STATUS_BAR_LUMINANCE_THRESHOLD)
                                IS_DARK_ALBUM else IS_LIGHT_ALBUM
                        )
                        if (card != null && !PrefManager.animationReduced(context)) {
                            val options = ActivityOptionsCompat.makeClipRevealAnimation(
                                card,
                                0, 0,
                                card.width,
                                card.height
                            )
                            context.startActivity(intent, options.toBundle())
                        } else {
                            context.startActivity(intent)
                        }
                    }
                }
            } else {
                // If no album art passed, calculate later in setupAppBar()
                intent.putExtra(
                    IS_DARK_ALBUM_ART_TAG,
                    NOT_SET
                )
                context.startActivity(intent)
            }
        }
    }

    private lateinit var albumDetailAdapter: AlbumDetailAdapter
    private lateinit var decorView: View
    private var songsInAlbum: MutableList<Audio> = mutableListOf()
    private val albumArtJob = SupervisorJob()
    private lateinit var metadata: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        if (threshold == null) {
            threshold = -(resources.displayMetrics.widthPixels shr 1)
            statusBarPosThreshold = -resources.displayMetrics.widthPixels * 17 / 20
        }

        decorView = window.decorView
        metadata = intent.getStringArrayExtra(METADATA_TAG)
        albumDetailAdapter = AlbumDetailAdapter(metadata[2])

        setUpUI()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        GlobalScope.launch(Dispatchers.Main) {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@AlbumDetailActivity)
                adapter = AlphaInAnimationAdapter(albumDetailAdapter).apply {
                    setFirstOnly(false)
                    setDuration(200)
                }
                setHasFixedSize(true)
            }
        }

        // load songs in album
        GlobalScope.launch(Dispatchers.IO + albumArtJob) {
            delay(200)
            songsInAlbum = MediaHunter.getAudioByAlbumId(this@AlbumDetailActivity, metadata[2])
            withContext(Dispatchers.Main) {
                albumDetailAdapter.setSongsInAlbum(songsInAlbum)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun collapse() {
        fab.hide()
    }

    private fun expand() {
        fab.show()
    }

    private fun setUpUI() {
        GlobalScope.launch(Dispatchers.IO + albumArtJob) {
            // set album art
            val albumArtUri = MediaHunter.getArtUriFromAlbumId(metadata[2].toLong())

            var bitmap: Bitmap

            try {
                val futureTarget: FutureTarget<Bitmap> = Glide.with(albumArt)
                    .asBitmap()
                    .load(albumArtUri)
                    .submit(720, 720)
                bitmap = futureTarget.get()
                Glide.with(albumArt).clear(futureTarget)

                toolbar.post {
                    intent.getIntExtra(IS_DARK_ALBUM_ART_TAG, NOT_SET).let {
                        if (it == NOT_SET) {
                            setUpAppBar(it, bitmap)
                        } else {
                            setUpAppBar(it)
                        }
                    }
                }

                // set fab color
                Palette.from(bitmap).generate { palette ->
                    val dominant = palette?.getDominantColor(Color.WHITE)
                    if (Generic.luminance(dominant ?: Color.WHITE) >= FAB_LUMINANCE_THRESHOLD) {
                        // dominant color is bright
                        val secondary = palette?.getDarkMutedColor(Generic.setAlpha(dominant!!, 128))
                        fab.imageTintList = secondary?.let { ColorStateList.valueOf(it) }
                        fab.backgroundTintList = dominant?.let { ColorStateList.valueOf(it) }
                    } else {
                        // dominant color is dark
                        val secondary = palette?.getLightVibrantColor(Color.WHITE)
                        fab.imageTintList = secondary?.let { ColorStateList.valueOf(it) }
                        fab.backgroundTintList = dominant?.let { ColorStateList.valueOf(it) }
                    }
                }
                withContext(Dispatchers.Main) {
                    albumArt.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.w("AlbumDetailActivity", "e.message")
                bitmap = getDrawable(R.drawable.default_album_art_blue)!!.toBitmap(512, 512)
                setUpAppBar(IS_DARK_ALBUM, bitmap)
            }

            withContext(Dispatchers.Main) {
                delay(300)
                fab.show()
            }
        }

        // set metadata
        albumTitle.text = metadata[0]
        description.text = metadata[1]
        toolbar.title = metadata[0]

        // set up fab
        fab.setOnClickListener {
            PlaybackManager.playAlbum(metadata[2])
        }
    }

    private fun setUpAppBar(isAlbumArtDark: Int, bitmap: Bitmap? = null) {
        val navIcon = toolbar.navigationIcon
        val menuIcon = toolbar.overflowIcon
        when (isAlbumArtDark) {
            IS_DARK_ALBUM -> {
                // change status bar color dynamically
                Generic.setLightStatusBar(decorView, false)
                navIcon?.colorFilter = whiteFilter
                menuIcon?.colorFilter = whiteFilter
                appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
                    private var cachedOffset = 0
                    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                        if (threshold!! in verticalOffset until cachedOffset) {
                            collapse()
                        } else if (threshold!! in (cachedOffset + 1)..verticalOffset) {
                            expand()
                        }

                        if (statusBarPosThreshold!! in verticalOffset until cachedOffset) {
                            // collapsing
                            navIcon?.clearColorFilter()
                            menuIcon?.clearColorFilter()
                            Generic.setLightStatusBar(decorView, true)
                        } else if (statusBarPosThreshold!! in (cachedOffset + 1)..verticalOffset) {
                            // expanding
                            navIcon?.colorFilter = whiteFilter
                            menuIcon?.colorFilter = whiteFilter
                            Generic.setLightStatusBar(decorView, false)
                        }
                        val opacity = (verticalOffset - threshold!!) / 200f
                        albumTitle.alpha = opacity
                        description.alpha = opacity
                        cachedOffset = verticalOffset
                    }
                })
            }

            IS_LIGHT_ALBUM -> {
                // always use light status bar (dark icon)

                // Generic.setLightStatusBar(decorView, true)
                appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
                    private var cachedOffset = 0
                    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                        if (threshold!! in verticalOffset until cachedOffset) {
                            collapse()
                        } else if (threshold!! in (cachedOffset + 1)..verticalOffset) {
                            expand()
                        }
                        val opacity = (verticalOffset - threshold!!) / 200f
                        albumTitle.alpha = opacity
                        description.alpha = opacity
                        cachedOffset = verticalOffset
                    }
                })
            }

            NOT_SET -> {
                bitmap?.let {
                    Palette.from(bitmap).generate { palette ->
                        GlobalScope.launch {
                            val primaryColor = palette?.getDominantColor(Color.WHITE) ?: Color.WHITE
                            if (Generic.luminance(primaryColor) < STATUS_BAR_LUMINANCE_THRESHOLD) {
                                setUpAppBar(IS_DARK_ALBUM)
                            } else {
                                setUpAppBar(IS_LIGHT_ALBUM)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_album_item, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.play -> {
                fab.callOnClick()
            }
            R.id.add_to_playlist -> {
                addToPlaylist(songsInAlbum)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addToPlaylist(audioList: List<Audio>) {
        GlobalScope.launch(Dispatchers.IO) {
            val playlistList = MediaHunter.getAllPlaylists(this@AlbumDetailActivity)

            playlistList.let { playlists ->
                // prepare playlist items
                val items = Array(playlists.size) { "1" }
                for (position in playlists.indices) {
                    items[position] = playlists[position].name.toString()
                }

                // show dialog
                MaterialAlertDialogBuilder(this@AlbumDetailActivity).apply {
                    setTitle("Add to playlist")
                    setItems(items) { dialog, position ->
                        val playlist = playlists[position]
                        Log.d("AddToPlaylist", playlist.toString())
                        GlobalScope.launch(Dispatchers.IO) {
                            if (MediaHunter.addToPlaylist(this@AlbumDetailActivity, playlist.id, audioList) == 0) {
                                withContext(Dispatchers.Main) {
                                    Snackbar.make(
                                        rootLayout,
                                        "Operation failed",
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) { show() }
                }
            }
        }
    }

    private inner class AlbumDetailAdapter(val albumId: String) :
        RecyclerView.Adapter<AlbumDetailAdapter.AudioViewHolder>() {
        private var audioItems: MutableList<Audio> = mutableListOf()

        inner class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val view = v
            val title: TextView = v.title
            val artist: TextView = v.description
            val duration: TextView = v.duration
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.album_detail_audio_item, parent, false)
            return AudioViewHolder(view)
        }

        override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
            holder.title.text = audioItems[position].title
            holder.artist.text = audioItems[position].artist
            holder.duration.text =
                Generic.msecToStr(audioItems[position].duration ?: 0)
            holder.view.setOnClickListener {
                PlaybackManager.loadAlbumAndSkipTo(
                    albumId,
                    holder.adapterPosition
                )
            }

            holder.view.setOnLongClickListener {
                PopupMenu(it.context, it.menuAnchor, Gravity.END).apply {
                    inflate(R.menu.menu_album_audio_item)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.play -> {
                                holder.view.callOnClick()
                            }
                            R.id.add_to_playlist -> {
                                addToPlaylist(listOf(audioItems[position]))
                            }
                            R.id.view_artist -> {
                                audioItems[position].artistId?.let {
                                    ArtistDetailActivity.start(
                                        this@AlbumDetailActivity,
                                        it, audioItems[position].artist
                                    )
                                }
                            }
                        }
                        true
                    }
                    show()
                }
                true
            }
        }

        override fun getItemCount(): Int = audioItems.size

        fun setSongsInAlbum(list: MutableList<Audio>) {
            audioItems = list
            notifyDataSetChanged()
        }
    }
}