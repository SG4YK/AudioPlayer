package com.github.sg4yk.audioplayer

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_album_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlbumDetailActivity : AppCompatActivity() {
    companion object {
        private var thresHold: Int? = null
        const val EXTRA_TAG = "ALBUM_EXTRA"
    }

    val adapter = AlbumDetailAdapter()

    val songsInAlbum: MutableLiveData<MutableList<MediaMetadataCompat>> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        loadData()

        if (thresHold == null) {
            thresHold = -getResources().getDisplayMetrics().widthPixels / 2
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val albumArt = findViewById<ImageView>(R.id.albumArt)

        val appBarLayout = findViewById<AppBarLayout>(R.id.app_bar)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            private var cachedOffset = 0
            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                if (cachedOffset > thresHold!! && verticalOffset <= thresHold!!) {
                    collapse()
                } else if (cachedOffset < thresHold!! && verticalOffset >= thresHold!!) {
                    expand()
                }
                val opacity = (verticalOffset - thresHold!!) / 200f
                albumTitle.alpha = opacity
                artist.alpha = opacity
                cachedOffset = verticalOffset
            }
        })

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
        val extras = intent.getStringArrayExtra(EXTRA_TAG)
        albumTitle.text = extras[0]
        toolbar.title = extras[0]
        artist.text = extras[1]
        toolbar.subtitle = extras[1]
        GlobalScope.launch(Dispatchers.Main) {
            val bitmap = MediaHunter.getThumbnail(this@AlbumDetailActivity, extras[2], 300)
            if (bitmap != null) {
                albumArt.setImageBitmap(bitmap)
            }
        }

        // load songs in album
        GlobalScope.launch(Dispatchers.IO) {
            delay(300)
            songsInAlbum.postValue(
                MediaHunter.getSongsInAlbumById(
                    this@AlbumDetailActivity,
                    Uri.parse(extras[2]).lastPathSegment!!
                )
            )
        }
        val observer = Observer<MutableList<MediaMetadataCompat>> {
            adapter.setSongsInAlbum(it)
        }
        songsInAlbum.observe(this@AlbumDetailActivity, observer)
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
