package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.media.Artist
import kotlinx.android.synthetic.main.artist_item.view.*

class ArtistItemAdapter : RecyclerView.Adapter<ArtistItemAdapter.ArtistViewHolder>() {
    private var artistItems: MutableList<Artist> = mutableListOf()
    private lateinit var context: Context

    class ArtistViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val artist: TextView = v.description
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.artist_item, parent, false)

        return ArtistViewHolder(view)
    }

    override fun getItemCount(): Int = artistItems.size

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.artist.text = artistItems[position].artist ?: "Unknown artist"
        holder.view.setOnClickListener {
            ArtistDetailActivity.start(context, artistItems[position])
        }
    }

    fun setArtistList(list: MutableList<Artist>) {
        artistItems = list
        notifyDataSetChanged()
    }

}