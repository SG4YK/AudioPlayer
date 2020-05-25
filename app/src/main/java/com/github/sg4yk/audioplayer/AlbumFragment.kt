package com.github.sg4yk.audioplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AlbumFragment : Fragment() {

    companion object {
        fun newInstance() = AlbumFragment()

    }

    private lateinit var viewModel: AppViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: AlbumItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.album_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = AlbumItemAdapter()

        GlobalScope.launch(Dispatchers.Main) {
            delay(300)
            recyclerView = view!!.findViewById(R.id.recycler_album)

            layoutManager = GridLayoutManager(activity, 2)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = AlphaInAnimationAdapter(adapter).apply {
                setFirstOnly(true)
                setDuration(300)
            }
            recyclerView.setHasFixedSize(true)
            viewModel = activity?.run {
                ViewModelProvider(this).get(AppViewModel::class.java)
            }!!
            val albumList = viewModel.albumItemsLiveData
            val observer = Observer<MutableList<AlbumItem>> {
                adapter.setAlbumItemList(it)
            }
            albumList.observe(viewLifecycleOwner, observer)
        }

    }

}
