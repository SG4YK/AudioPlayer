package com.github.sg4yk.audioplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.media.Audio
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.library_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LibraryFragment : Fragment() {

    companion object {
        fun newInstance() = LibraryFragment()
    }

    private lateinit var viewModel: AppViewModel
    private val audioItemAdapter = AudioItemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.library_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = AlphaInAnimationAdapter(audioItemAdapter).apply {
                    setFirstOnly(true)
                    setDuration(300)
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
}
