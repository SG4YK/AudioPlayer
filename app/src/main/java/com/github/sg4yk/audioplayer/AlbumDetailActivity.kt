package com.github.sg4yk.audioplayer

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_album_detail.*
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

    private val adapter = AlbumDetailAdapter()
    private lateinit var decorView: View
    private var navIcon: Drawable? = null
    private val songsInAlbum: MutableLiveData<MutableList<MediaMetadataCompat>> = MutableLiveData()
    private val albumArtJob = SupervisorJob()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        decorView = window.decorView

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

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Not implemented yet", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        GlobalScope.launch(Dispatchers.Main) {
            val layoutManager = LinearLayoutManager(this@AlbumDetailActivity)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = AlphaInAnimationAdapter(adapter).apply {
                setFirstOnly(false)
                setDuration(300)
            }
            recyclerView.setHasFixedSize(true)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadData() {
        // load album metadata
        val extras = intent.getStringArrayExtra(METADATA_TAG)

        GlobalScope.launch(Dispatchers.IO + albumArtJob) {
            // set album art
            val albumArtUri = MediaHunter.getArtUriFromAlbumId(extras[2].toLong() ?: -1)

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
                    val primary = palette?.getDominantColor(Color.WHITE)
                    if (Generic.luminance(primary ?: Color.WHITE) >= FAB_LUMINANCE_THRESHOLD) {
                        // dominant color is bright
                        var secondary = palette?.getDarkMutedColor(Generic.setAlpha(primary!!, 128))
                        fab.imageTintList = secondary?.let { ColorStateList.valueOf(it) }
                        fab.backgroundTintList = primary?.let { ColorStateList.valueOf(it) }
                    } else {
                        // dominant color is dark
                        var secondary = palette?.getLightVibrantColor(Color.WHITE)
                        fab.imageTintList = secondary?.let { ColorStateList.valueOf(it) }
                        fab.backgroundTintList = primary?.let { ColorStateList.valueOf(it) }
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
        albumTitle.text = extras[0]
        artist.text = extras[1]
        toolbar.title = extras[0]

        // load songs in album
        GlobalScope.launch(Dispatchers.IO + albumArtJob) {
            delay(300)
            songsInAlbum.postValue(
                MediaHunter.getSongsInAlbumById(
                    this@AlbumDetailActivity,
                    extras[2]
                )
            )
        }
        val observer = Observer<MutableList<MediaMetadataCompat>> {
            adapter.setSongsInAlbum(it)
        }
        songsInAlbum.observe(this@AlbumDetailActivity, observer)
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
                    if (cachedOffset > threshold!! && verticalOffset <= threshold!!) {
                        collapse()
                    } else if (cachedOffset < threshold!! && verticalOffset >= threshold!!) {
                        expand()
                    }

                    if (cachedOffset > statusBarPosThreshold!! && verticalOffset <= statusBarPosThreshold!!) {
                        // collapsing
                        navIcon?.clearColorFilter()
                        Generic.setLightStatusBar(decorView, true)
                    } else if (cachedOffset < statusBarPosThreshold!! && verticalOffset >= statusBarPosThreshold!!) {
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

class AlbumDetailAdapter : RecyclerView.Adapter<AlbumDetailAdapter.AudioViewHolder>() {
    var audioItems: MutableList<MediaMetadataCompat> = mutableListOf()

    class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.findViewById(R.id.title)
        val artist: TextView = v.findViewById(R.id.artist)
        val duration: TextView = v.findViewById(R.id.duration)
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
    }

    override fun getItemCount(): Int {
        return audioItems.size
    }

    fun setSongsInAlbum(list: MutableList<MediaMetadataCompat>) {
        audioItems = list
        notifyDataSetChanged()
    }
}