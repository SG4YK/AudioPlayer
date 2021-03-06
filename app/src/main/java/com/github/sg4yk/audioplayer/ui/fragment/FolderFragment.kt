package com.github.sg4yk.audioplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.sg4yk.audioplayer.utils.AppViewModel
import com.github.sg4yk.audioplayer.R


class FolderFragment : Fragment() {

    companion object {
        fun newInstance() = FolderFragment()
    }

    private lateinit var viewModel: AppViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.folder_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this).get(AppViewModel::class.java)
        }!!
    }

}
