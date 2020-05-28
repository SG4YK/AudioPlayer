package com.github.sg4yk.audioplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.github.sg4yk.audioplayer.media.Album
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.album_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AlbumFragment : Fragment() {

    companion object {
        fun newInstance() = AlbumFragment()
    }

    private lateinit var viewModel: AppViewModel
    private lateinit var albumItemAdapter: AlbumItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.album_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        albumItemAdapter = AlbumItemAdapter()

        GlobalScope.launch(Dispatchers.Main) {
            delay(300)
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

            val albumList = viewModel.albumItemsLiveData
            val observer = Observer<MutableList<Album>> {
                albumItemAdapter.setAlbums(it)
            }
            albumList.observe(viewLifecycleOwner, observer)
        }
    }
}
