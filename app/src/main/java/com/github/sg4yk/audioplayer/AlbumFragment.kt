package com.github.sg4yk.audioplayer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.album_fragment.*
import kotlinx.android.synthetic.main.album_item.view.*
import kotlinx.coroutines.*


class AlbumFragment : Fragment() {

    companion object {
        fun newInstance() = AlbumFragment()
    }

    private lateinit var viewModel: AppViewModel
    private lateinit var albumItemAdapter: AlbumItemAdapterExt
    private lateinit var v: View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = inflater.inflate(R.layout.album_fragment, container, false)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        albumItemAdapter = AlbumItemAdapterExt()

        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            recyclerView.apply {
                layoutManager = GridLayoutManager(activity, 2)
                adapter = AlphaInAnimationAdapter(albumItemAdapter).apply {
                    setFirstOnly(true)
                    setDuration(200)
                }
                setHasFixedSize(true)
            }

            viewModel = activity?.run {
                ViewModelProvider(this).get(AppViewModel::class.java)
            }!!

            delay(200)

            val albumList = viewModel.albumItemsLiveData
            val observer = Observer<MutableList<Album>> {
                albumItemAdapter.setAlbums(it)
            }
            albumList.observe(viewLifecycleOwner, observer)
        }
    }

    private inner class AlbumItemAdapterExt : AlbumItemAdapter() {
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
            val playlistList = viewModel.playlistItemsLiveData.value
            playlistList?.let { playlists ->
                val items = Array(playlists.size) { "1" }
                for (position in playlists.indices) {
                    items[position] = playlists[position].name.toString()
                }
                MaterialAlertDialogBuilder(ctx).apply {
                    setTitle("Add to playlist")
                    setItems(items) { dialog, position ->
                        val playlist = playlists[position]
                        Log.d("AddToPlaylist", playlist.toString())
                        GlobalScope.launch(Dispatchers.IO) {
                            val audioList = MediaHunter.getAudioByAlbumId(ctx, album.id.toString())
                            if (MediaHunter.addToPlaylist(context, playlist.id, audioList) == 0) {
                                withContext(Dispatchers.Main) {
                                    Snackbar.make(v, "Operation failed", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    show()
                }
            }
        }
    }
}
