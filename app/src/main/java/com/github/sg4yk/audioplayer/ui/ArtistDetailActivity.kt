package com.github.sg4yk.audioplayer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.media.Artist
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.playback.PlaybackManager
import com.github.sg4yk.audioplayer.ui.adapter.AlbumItemAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_artist_detail.*
import kotlinx.android.synthetic.main.album_item.view.*
import kotlinx.coroutines.*

class ArtistDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TAG = "ARTIST_DETAIL_EXTRA"

        fun start(ctx: Context, artistId: Long, artistName: String?) {
            val intent = Intent(ctx, ArtistDetailActivity::class.java).apply {
                putExtra(
                    EXTRA_TAG,
                    arrayOf(
                        artistId.toString(),
                        artistName ?: "Unknown artist"
                    )
                )
            }
            ctx.startActivity(intent)
        }


        fun start(ctx: Context, artist: Artist) {
            start(
                ctx,
                artist.id,
                artist.artist
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artist_detail)
        val extras = intent.getStringArrayExtra(EXTRA_TAG)

        toolbar.post {
            toolbar.title = extras[1]
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        val albumItemAdapter = AlbumItemAdapterExt(this)

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@ArtistDetailActivity, 2)
            adapter = AlphaInAnimationAdapter(albumItemAdapter).apply {
                setFirstOnly(true)
                setDuration(200)
            }
            setHasFixedSize(true)
        }


        GlobalScope.launch(Dispatchers.IO) {
            delay(50)
            val albumList = MediaHunter.getAlbumsByArtistId(this@ArtistDetailActivity, extras[0].toLong())
            withContext(Dispatchers.Main) {
                albumItemAdapter.setAlbums(albumList)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private inner class AlbumItemAdapterExt(val context: Context) : AlbumItemAdapter() {
        override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.view.setOnLongClickListener {
                PopupMenu(it.context, it.menuAnchor, Gravity.END).apply {
                    inflate(R.menu.menu_album_item)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.play -> {
                                PlaybackManager.playAlbum(albumList[position].id.toString())
                            }
                            R.id.add_to_playlist -> {
                                addToPlaylist(it.context, albumList[position])
                            }
                        }
                        true
                    }
                    show()
                }
                true
            }
        }

        private fun addToPlaylist(ctx: Context, album: Album) {
            GlobalScope.launch(Dispatchers.IO) {
                val playlistList = MediaHunter.getAllPlaylists(ctx)
                playlistList.let { playlists ->
                    val items = Array(playlists.size) { "1" }
                    for (position in playlists.indices) {
                        items[position] = playlists[position].name.toString()
                    }

                    MaterialAlertDialogBuilder(ctx).apply {
                        setTitle("Add to playlist")
                        setItems(items) { dialog, position ->
                            val playlist = playlists[position]
                            GlobalScope.launch(Dispatchers.IO) {
                                val audioList = MediaHunter.getAudioByAlbumId(ctx, album.id.toString())
                                if (MediaHunter.addToPlaylist(ctx, playlist.id, audioList) == 0) {
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
    }
}
