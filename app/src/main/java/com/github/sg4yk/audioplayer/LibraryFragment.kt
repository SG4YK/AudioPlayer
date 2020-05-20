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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LibraryFragment : Fragment() {

    companion object {
        fun newInstance() = LibraryFragment()
        private val adapter = AudioItemAdapter()
    }

    private lateinit var viewModel: AppViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.library_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProvider(this).get(AppViewModel::class.java)

        recyclerView = view!!.findViewById(R.id.recycler_library)
        layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this).get(AppViewModel::class.java)
        val mediaList = viewModel.audioItemsLiveData
        val observer = Observer<List<AudioItem>> {

//            GlobalScope.launch(Dispatchers.Main) {
            adapter.setAudioItemList(it)
//            }
        }
        mediaList.observe(viewLifecycleOwner, observer)
    }

    override fun onStart() {
        super.onStart()


    }
}
