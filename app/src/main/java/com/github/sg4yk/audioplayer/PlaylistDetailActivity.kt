package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.media.Playlist
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.google.android.material.appbar.AppBarLayout
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_playlist_detail.*
import kotlinx.android.synthetic.main.album_detail_audio_item.view.*
import kotlinx.coroutines.*

class PlaylistDetailActivity : AppCompatActivity() {
    companion object {
        private var threshold: Int? = null
        const val EXTRA_TAG = "PLAYLIST_EXTRA"

        fun start(context: Context, playlist: Playlist) {
            val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                putExtra(
                    PlaylistDetailActivity.EXTRA_TAG,
                    arrayOf(
                        playlist.id.toString(),
                        playlist.name
                    )
                )
            }
            context.startActivity(intent)
        }
    }

    private lateinit var extras: Array<String>
    private val playlistDetailJob = SupervisorJob()
    private var audioInPlaylist: MutableList<Audio> = mutableListOf()
    private lateinit var playlistDetailAdapter: PlaylistDetailAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        extras = intent.getStringArrayExtra(EXTRA_TAG)
        playlistDetailAdapter = PlaylistDetailAdapter(extras[0])

        if (threshold == null) {
            threshold = -(resources.displayMetrics.widthPixels shr 1)
        }

        toolbar.post {
            toolbar.title = extras[1]
            name.text = extras[1]
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            setUpAppBar()
        }

        // load songs in playlist
        GlobalScope.launch(Dispatchers.IO + playlistDetailJob) {
            async {
                audioInPlaylist = MediaHunter.getAudioByPlayListId(this@PlaylistDetailActivity, extras[0].toLong())
                setAlbumArt(audioInPlaylist)
                delay(200)
                withContext(Dispatchers.Main) {
                    playlistDetailAdapter.setSongsInAlbum(audioInPlaylist)
                }
            }

            withContext(Dispatchers.Main) {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(this@PlaylistDetailActivity)
                    adapter = AlphaInAnimationAdapter(playlistDetailAdapter).apply {
                        setFirstOnly(false)
                        setDuration(200)
                    }
                    setHasFixedSize(true)
                }
                fab.setOnClickListener {
                    PlaybackManager.playPlaylist(extras[0])
                }
                delay(300)
                fab.show()
            }
        }


        setUpAppBar()
    }

    private fun setAlbumArt(audioList: MutableList<Audio>) {
        if (audioList.size == 0) {
            return
        }
        val albumArts = albumArtContainer.children.toList()
        GlobalScope.launch {
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            for (i in albumArts.indices) {
                async {
                    audioList[i % audioList.size].albumId?.let {
                        val view = albumArts[i] as ImageView
                        val uri = MediaHunter.getArtUriFromAlbumId(it)
                        withContext(Dispatchers.Main) {
                            Glide.with(view)
                                .load(uri)
//                                .transition(withCrossFade())
                                .error(R.drawable.default_album_art_blue)
                                .into(view)
                        }
                    }
                }
            }
        }
    }

    private fun setUpAppBar() {
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            private var cachedOffset = 0
            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                if (threshold!! in verticalOffset until cachedOffset) {
                    fab.hide()
                } else if (threshold!! in (cachedOffset + 1)..verticalOffset) {
                    fab.show()
                }
                val opacity = (verticalOffset - threshold!!) / 200f
                name.alpha = opacity
                cachedOffset = verticalOffset
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private class PlaylistDetailAdapter(val playlistId: String) :
        RecyclerView.Adapter<PlaylistDetailAdapter.AudioViewHolder>() {
        private var audioItems: MutableList<Audio> = mutableListOf()

        class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val view = v
            val title: TextView = v.title
            val description: TextView = v.description
            val duration: TextView = v.duration
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.album_detail_audio_item, parent, false)
            return AudioViewHolder(view)
        }

        override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
            holder.title.text = audioItems[position].title
            holder.description.text = "${audioItems[position].artist} - ${audioItems[position].album}"
            holder.duration.text =
                Generic.msecToStr(audioItems[position].duration ?: 0)
            holder.view.setOnClickListener {
                PlaybackManager.loadPlaylistAndSkipTo(
                    playlistId,
                    holder.adapterPosition
                )
            }
        }

        override fun getItemCount(): Int = audioItems.size

        fun setSongsInAlbum(list: MutableList<Audio>) {
            audioItems = list
            notifyDataSetChanged()
        }
    }
}
