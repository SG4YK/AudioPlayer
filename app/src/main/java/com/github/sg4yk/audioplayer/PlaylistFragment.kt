package com.github.sg4yk.audioplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sg4yk.audioplayer.media.Playlist
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.playlist_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaylistFragment : Fragment() {

    companion object {
        fun newInstance() = PlaylistFragment()
    }

    private lateinit var viewModel: AppViewModel
    private val playlistItemAdapter = PlaylistItemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.playlist_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = AlphaInAnimationAdapter(playlistItemAdapter).apply {
                    setFirstOnly(false)
                    setDuration(200)
                }
                setHasFixedSize(true)
            }


            viewModel = activity?.run {
                ViewModelProvider(this).get(AppViewModel::class.java)
            }!!

            delay(200)

            val observer = Observer<MutableList<Playlist>> {
                playlistItemAdapter.setPlaylistList(it)
            }
            viewModel.playlistItemsLiveData.observe(viewLifecycleOwner, observer)
        }
    }
}