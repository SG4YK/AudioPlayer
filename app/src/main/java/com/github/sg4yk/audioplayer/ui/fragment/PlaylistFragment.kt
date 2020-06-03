package com.github.sg4yk.audioplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.github.sg4yk.audioplayer.utils.AppViewModel
import com.github.sg4yk.audioplayer.ui.adapter.PlaylistItemAdapter
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.media.Playlist
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.playback.PlaybackManager
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.playlist_fragment.*
import kotlinx.android.synthetic.main.playlist_item.view.*
import kotlinx.coroutines.*

class PlaylistFragment : Fragment() {

    companion object {
        fun newInstance() = PlaylistFragment()
    }

    private lateinit var viewModel: AppViewModel
    private lateinit var playlistItemAdapter: PlaylistItemAdapterExt

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.playlist_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        playlistItemAdapter = PlaylistItemAdapterExt(view!!)
        viewModel = activity?.run {
            ViewModelProvider(this).get(AppViewModel::class.java)
        }!!

        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = AlphaInAnimationAdapter(playlistItemAdapter).apply {
                    setFirstOnly(true)
                    setDuration(200)
                }
                setHasFixedSize(true)
            }

            delay(200)

            val observer = Observer<MutableList<Playlist>> {
                playlistItemAdapter.setPlaylistList(it)
            }
            viewModel.playlistItemsLiveData.observe(viewLifecycleOwner, observer)
        }
    }

    private inner class PlaylistItemAdapterExt(val v: View) : PlaylistItemAdapter() {
        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.view.setOnLongClickListener {
                PopupMenu(it.context, it.menuAnchor, Gravity.END).apply {
                    inflate(R.menu.menu_playlist_item)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.play -> {
                                PlaybackManager.playPlaylist(playlistItems[position].id.toString())
                            }
                            R.id.rename -> {
                                renamePlaylist(context, playlistItems[position])
                            }
                            R.id.delete -> {
                                deletePlaylist(context, playlistItems[position])
                            }
                        }
                        true
                    }
                    show()
                }
                true
            }
        }

        private fun deletePlaylist(ctx: Context, playlist: Playlist) {
            MaterialDialog(ctx).show {
                title(null, "Delete playlist")
                message(null, "Delete ${playlist.name} ?")
                negativeButton(R.string.cancel)
                positiveButton(null, "Delete") {
                    GlobalScope.launch(Dispatchers.IO) {
                        val res = MediaHunter.deletePlaylist(ctx, playlist.id.toString())
                        withContext(Dispatchers.Main) {
                            if (res == 0) {
                                Snackbar.make(v, "Operation failed", Snackbar.LENGTH_LONG).show()
                            } else {
                                viewModel.refreshPlaylistItems()
                            }
                        }
                    }
                }
            }
        }

        private fun renamePlaylist(ctx: Context, playlist: Playlist) {
            MaterialDialog(ctx).show {
                input(maxLength = 30, prefill = playlist.name)
                title(null, "Rename playlist")
                getInputLayout().apply {
                    hint = "Playlist name"
                    setStartIconDrawable(R.drawable.ic_playlist_play_white_24dp)
                    negativeButton(R.string.cancel)
                    positiveButton(null, "Rename") {
                        GlobalScope.launch(Dispatchers.IO) {
                            val res = MediaHunter.renamePlaylist(
                                ctx, playlist.id.toString(),
                                it.getInputField().text.toString()
                            )
                            withContext(Dispatchers.Main) {
                                if (res == 0) {
                                    Snackbar.make(v, "Operation failed", Snackbar.LENGTH_LONG).show()
                                } else {
                                    viewModel.refreshPlaylistItems()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}