package com.github.sg4yk.audioplayer

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.google.android.material.appbar.AppBarLayout
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_album_detail.*
import kotlinx.android.synthetic.main.album_detail_audio_item.view.*
import kotlinx.coroutines.*

class AlbumDetailActivity : AppCompatActivity() {
    companion object {
        private var threshold: Int? = null
        private var statusBarPosThreshold: Int? = null
        const val STATUS_BAR_LUMINANCE_THRESHOLD = 128
        const val FAB_LUMINANCE_THRESHOLD = 128
        const val METADATA_TAG = "EXTRA_METADATA"
        const val IS_DARK_ALBUM_ART_TAG = "EXTRA_IS_DARK_ALBUM_ART"
        private val whiteFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.DST)
    }

    private lateinit var albumDetailAdapter: AlbumDetailAdapter
    private lateinit var decorView: View
    private var navIcon: Drawable? = null
    private var songsInAlbum: MutableList<MediaMetadataCompat> = mutableListOf()
    private val albumArtJob = SupervisorJob()
    private lateinit var metadata: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        decorView = window.decorView
        metadata = intent.getStringArrayExtra(METADATA_TAG)
        albumDetailAdapter = AlbumDetailAdapter(metadata[2])

        toolbar.post {
            setUpAppBar(intent.getBooleanExtra(IS_DARK_ALBUM_ART_TAG, false))
        }

        loadData()

        if (threshold == null) {
            threshold = -(resources.displayMetrics.widthPixels shr 1)
            statusBarPosThreshold = -resources.displayMetrics.widthPixels * 17 / 20
        }

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

    //    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadData() {
        GlobalScope.launch(Dispatchers.IO + albumArtJob) {
            // set album art
            val albumArtUri = MediaHunter.getArtUriFromAlbumId(metadata[2].toLong())

            var bitmap = getDrawable(R.drawable.default_album_art_blue)!!.toBitmap(512, 512)

            try {
                val futureTarget: FutureTarget<Bitmap> = Glide.with(albumArt)
                    .asBitmap()
                    .load(albumArtUri)
                    .submit(720, 720)
                bitmap = futureTarget.get()
                Glide.with(albumArt).clear(futureTarget)

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
            }

            withContext(Dispatchers.Main) {
                delay(300)
                fab.show()
            }
        }

        // load metadata
        albumTitle.text = metadata[0]
        artist.text = metadata[1]
        toolbar.title = metadata[0]

        // load songs in album
        GlobalScope.launch(Dispatchers.IO + albumArtJob) {
            delay(200)
            songsInAlbum = MediaHunter.getAudioByAlbumId(this@AlbumDetailActivity, metadata[2])
            withContext(Dispatchers.Main) {
                albumDetailAdapter.setSongsInAlbum(songsInAlbum)
            }
        }

        // set up fab
        fab.setOnClickListener {
            PlaybackManager.playAlbum(metadata[2])
        }
    }

    private fun setUpAppBar(isAlbumArtDark: Boolean) {
        navIcon = toolbar.navigationIcon
        if (isAlbumArtDark) {
            // change status bar color dynamically
            Generic.setLightStatusBar(decorView, false)
            navIcon?.colorFilter = whiteFilter
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
                        Generic.setLightStatusBar(decorView, true)
                    } else if (statusBarPosThreshold!! in (cachedOffset + 1)..verticalOffset) {
                        // expanding
                        navIcon?.colorFilter = whiteFilter
                        Generic.setLightStatusBar(decorView, false)
                    }
                    val opacity = (verticalOffset - threshold!!) / 200f
                    albumTitle.alpha = opacity
                    artist.alpha = opacity
                    cachedOffset = verticalOffset
                }
            })
        } else {
            // always use light status bar (dark icon)

//            Generic.setLightStatusBar(decorView, true)
            appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
                private var cachedOffset = 0
                override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                    if (cachedOffset > threshold!! && verticalOffset <= threshold!!) {
                        collapse()
                    } else if (cachedOffset < threshold!! && verticalOffset >= threshold!!) {
                        expand()
                    }
                    val opacity = (verticalOffset - threshold!!) / 200f
                    albumTitle.alpha = opacity
                    artist.alpha = opacity
                    cachedOffset = verticalOffset
                }
            })
        }
    }
}

class AlbumDetailAdapter(val albumId: String) : RecyclerView.Adapter<AlbumDetailAdapter.AudioViewHolder>() {
    var audioItems: MutableList<MediaMetadataCompat> = mutableListOf()

    class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.title
        val artist: TextView = v.artist
        val duration: TextView = v.duration
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_detail_audio_item, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.title.text = audioItems[position].description.title
        holder.artist.text = audioItems[position].description.subtitle
        holder.duration.text =
            Generic.msecToStr(audioItems[position].getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
        holder.view.setOnClickListener {
            audioItems[position].description.mediaId?.let { audioId ->
                PlaybackManager.loadAlbumAndSkipTo(
                    albumId,
                    audioId
                )
            }
        }
    }

    override fun getItemCount(): Int = audioItems.size


    fun setSongsInAlbum(list: MutableList<MediaMetadataCompat>) {
        audioItems = list
        notifyDataSetChanged()
    }
}