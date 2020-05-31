package com.github.sg4yk.audioplayer.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.sg4yk.audioplayer.*
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.ui.AlbumDetailActivity
import com.github.sg4yk.audioplayer.ui.ArtistDetailActivity
import com.github.sg4yk.audioplayer.ui.adapter.AudioItemAdapter
import com.github.sg4yk.audioplayer.utils.AppViewModel
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.audio_item.view.*
import kotlinx.android.synthetic.main.library_fragment.*
import kotlinx.coroutines.*


class LibraryFragment : Fragment() {

    companion object {
        fun newInstance() = LibraryFragment()
    }

    private lateinit var viewModel: AppViewModel
    private lateinit var audioItemAdapter: AudioItemAdapterExt
    private lateinit var v: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = inflater.inflate(R.layout.library_fragment, container, false)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        audioItemAdapter = AudioItemAdapterExt()
        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = AlphaInAnimationAdapter(audioItemAdapter).apply {
                    setFirstOnly(true)
                    setDuration(200)
                }
                setHasFixedSize(true)
            }

            viewModel = activity?.run {
                ViewModelProvider(this).get(AppViewModel::class.java)
            }!!

            delay(200)

            val observer = Observer<MutableList<Audio>> {
                audioItemAdapter.setAudioItemList(it)
            }
            viewModel.audioItemsLiveData.observe(viewLifecycleOwner, observer)
        }
    }

    private inner class AudioItemAdapterExt : AudioItemAdapter() {
        override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    Glide.with(holder.albumArt)
                        .load(MediaHunter.getArtUriFromAlbumId(audioItems[position].albumId ?: -1))
                        .placeholder(R.drawable.default_album_art_blue)
                        .thumbnail(0.25f)
                        .centerInside()
                        .into(holder.albumArt)
                } catch (e: Exception) {
                    Log.w("AudioItemAdapter", e.message)
                }
            }

            val item = audioItems[position]
            holder.view.setOnLongClickListener {
                PopupMenu(it.context, it.menuAnchor, Gravity.END).apply {
                    inflate(R.menu.menu_audio_item)
                    setOnMenuItemClickListener { it ->
                        when (it.itemId) {
                            R.id.play -> {
                                holder.view.callOnClick()
                            }
                            R.id.add_to_playlist -> {
                                addToPlaylist(item)
                            }
                            R.id.view_artist -> {
                                item.artistId?.let {
                                    ArtistDetailActivity.start(
                                        context,
                                        it, item.artist
                                    )
                                }
                            }
                            R.id.view_album -> {
                                item.albumId?.let { albumId ->
                                    AlbumDetailActivity.start(
                                        context = context,
                                        albumId = albumId,
                                        albumArt = holder.albumArt.drawable.toBitmap()
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
    }

    private fun addToPlaylist(audio: Audio) {
        addToPlaylist(listOf(audio))
    }

    private fun addToPlaylist(audioList: List<Audio>) {
        val playlistList = viewModel.playlistItemsLiveData.value
        playlistList?.let { playlists ->
            val items = Array(playlists.size) { "1" }
            for (position in playlists.indices) {
                items[position] = playlists[position].name.toString()
            }
            context?.let { context ->
                MaterialAlertDialogBuilder(context).apply {
                    setTitle("Add to playlist")
                    setItems(items) { dialog, position ->
                        val playlist = playlists[position]
                        GlobalScope.launch(Dispatchers.IO) {
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
