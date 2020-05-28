package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.media.Playlist
import kotlinx.android.synthetic.main.playlist_item.view.*

class PlaylistItemAdapter : RecyclerView.Adapter<PlaylistItemAdapter.PlaylistViewHolder>() {
    private var playlistItems: MutableList<Playlist> = mutableListOf()
    private lateinit var context: Context

    class PlaylistViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val name = v.name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.playlist_item, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun getItemCount(): Int = playlistItems.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.name.text = playlistItems[position].name ?: ""
        holder.view.setOnClickListener {
            context.startActivity(Intent(context, PlaylistDetailActivity::class.java))
        }
    }

    fun setPlaylistList(list: MutableList<Playlist>) {
        playlistItems = list
        notifyDataSetChanged()
    }
}