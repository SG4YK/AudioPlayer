package com.github.sg4yk.audioplayer

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class PlaylistFragment : Fragment() {

    companion object {
        fun newInstance() = PlaylistFragment()
    }

    private lateinit var viewModel: PlaylistViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.playlist_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PlaylistViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
