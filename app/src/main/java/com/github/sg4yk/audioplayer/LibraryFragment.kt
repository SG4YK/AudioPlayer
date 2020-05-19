package com.github.sg4yk.audioplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.media.MetadataSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LibraryFragment : Fragment() {

    companion object {
        fun newInstance() = LibraryFragment()
    }

    private lateinit var viewModel: LibraryViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.library_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(LibraryViewModel::class.java)

        val recyclerView: RecyclerView = view!!.findViewById(R.id.recycler_library)
        val adapter = AudioItemAdapter()
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

//        val dividerItemDecoration = DividerItemDecoration(
//            recyclerView.context,
//            layoutManager.orientation
//        )
//        recyclerView.addItemDecoration(dividerItemDecoration)

        if (context != null) {
            val mediaSource = MetadataSource(context!!)
            GlobalScope.launch {
                mediaSource.load()
                mediaSource.whenReady {
                    adapter.setAudioList(mediaSource.toList())
                }
            }
        }
    }
}
