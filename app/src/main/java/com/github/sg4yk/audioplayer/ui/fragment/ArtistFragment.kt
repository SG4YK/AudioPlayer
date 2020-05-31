package com.github.sg4yk.audioplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sg4yk.audioplayer.utils.AppViewModel
import com.github.sg4yk.audioplayer.ui.adapter.ArtistItemAdapter
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.media.Artist
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.artist_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ArtistFragment : Fragment() {

    companion object {
        fun newInstance() = ArtistFragment()
    }

    private lateinit var viewModel: AppViewModel
    private val artistItemAdapter = ArtistItemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.artist_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = AlphaInAnimationAdapter(artistItemAdapter).apply {
                    setFirstOnly(false)
                    setDuration(200)
                }
                setHasFixedSize(true)
            }

            viewModel = activity?.run {
                ViewModelProvider(this).get(AppViewModel::class.java)
            }!!

            delay(200)

            val observer = Observer<MutableList<Artist>> {
                artistItemAdapter.setArtistList(it)
            }
            viewModel.artistItemsLiveData.observe(viewLifecycleOwner, observer)
        }
    }
}