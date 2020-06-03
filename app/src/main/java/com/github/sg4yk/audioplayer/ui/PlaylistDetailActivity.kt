package com.github.sg4yk.audioplayer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.media.Playlist
import com.github.sg4yk.audioplayer.media.PlaylistAudio
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.playback.PlaybackManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
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
                    EXTRA_TAG,
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
    private var audioInPlaylist: MutableList<PlaylistAudio> = mutableListOf()
    private lateinit var playlistDetailAdapter: PlaylistDetailAdapter
    private lateinit var playlistId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        extras = intent.getStringArrayExtra(EXTRA_TAG)
        playlistId = extras[0]
        playlistDetailAdapter = PlaylistDetailAdapter(playlistId)

        if (threshold == null) {
            threshold = -(resources.displayMetrics.widthPixels shr 1)
        }

        toolbar.post {
            toolbar.title = extras[1]
            name.text = extras[1]
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            toolbar.inflateMenu(R.menu.menu_album_item)
            setUpAppBar()
        }

        // load songs in playlist
        GlobalScope.launch(Dispatchers.IO + playlistDetailJob) {
            async {
                loadAudio()
            }

            withContext(Dispatchers.Main) {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(this@PlaylistDetailActivity)
                    adapter = AlphaInAnimationAdapter(playlistDetailAdapter).apply {
                        setFirstOnly(true)
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

    private suspend fun loadAudio() {
        audioInPlaylist =
            MediaHunter.getPlaylistAudioByPlayListId(this@PlaylistDetailActivity, extras[0].toLong())
        setAlbumArt(audioInPlaylist)
        delay(200)
        withContext(Dispatchers.Main) {
            playlistDetailAdapter.setSongsInPlaylist(audioInPlaylist)
        }
    }

    private fun setAlbumArt(audioList: MutableList<PlaylistAudio>) {
        if (audioList.size == 0) {
            return
        }
        val albumArts = albumArtContainer.children.toList()
        GlobalScope.launch {
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            for (i in albumArts.indices) {
                async {
                    audioList[i % audioList.size].audio.albumId?.let {
                        val view = albumArts[i] as ImageView
                        val uri = MediaHunter.getArtUriFromAlbumId(it)
                        withContext(Dispatchers.Main) {
                            Glide.with(view)
                                .load(uri)
                                .transition(withCrossFade())
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

    private fun removeFromPlaylist(audio: PlaylistAudio) {
        MaterialDialog(this).show {
            title(null, "Remove from playlist")
            message(
                null, "${audio.audio.title}\nRemove from current playlist?\n\n" +
                        "This action will not remove the audio from library."
            )

            negativeButton(R.string.cancel)
            positiveButton(null, "Remove") {
                GlobalScope.launch(Dispatchers.IO) {
                    val res = MediaHunter.removeFromPlaylist(
                        this@PlaylistDetailActivity,
                        playlistId.toLong(), audio.id
                    )
                    withContext(Dispatchers.Main) {
                        if (res == 1) {
                            loadAudio()
                        } else {
                            Snackbar.make(
                                rootLayout, "Operation failed",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_playlist_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.play -> {
                fab.callOnClick()
            }
            R.id.rename -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class PlaylistDetailAdapter(val playlistId: String) :
        RecyclerView.Adapter<PlaylistDetailAdapter.AudioViewHolder>() {
        private var audioItems: MutableList<PlaylistAudio> = mutableListOf()

        inner class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
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
            val item = audioItems[position]
            holder.title.text = item.audio.title
            holder.description.text = "${item.audio.artist} - ${item.audio.album}"
            holder.duration.text = Generic.msecToStr(item.audio.duration ?: 0)

            holder.view.setOnClickListener {
                PlaybackManager.loadPlaylistAndSkipTo(
                    playlistId,
                    holder.adapterPosition
                )
            }

            holder.view.setOnLongClickListener {
                PopupMenu(it.context, it.menuAnchor, Gravity.END).apply {
                    inflate(R.menu.menu_playlist_audio_item)
                    setOnMenuItemClickListener { it ->
                        when (it.itemId) {
                            R.id.play -> {
                                holder.view.callOnClick()
                            }

                            R.id.view_artist -> {
                                item.audio.artistId?.let { artistId ->
                                    ArtistDetailActivity.start(
                                        this@PlaylistDetailActivity,
                                        artistId = artistId,
                                        artistName = item.audio.artist
                                    )
                                }
                            }

                            R.id.view_album -> {
                                item.audio.albumId?.let {
                                    AlbumDetailActivity.start(
                                        this@PlaylistDetailActivity,
                                        albumId = item.audio.albumId
                                    )
                                }
                            }

                            R.id.remove -> {
                                removeFromPlaylist(item)
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

        fun setSongsInPlaylist(list: MutableList<PlaylistAudio>) {
            audioItems = list
            notifyDataSetChanged()
        }
    }
}
