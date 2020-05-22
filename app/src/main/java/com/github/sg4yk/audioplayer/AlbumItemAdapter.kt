package com.github.sg4yk.audioplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlbumItemAdapter : RecyclerView.Adapter<AlbumItemAdapter.AlbumViewHolder>() {
    private var albumItemList: MutableList<AlbumItem> = mutableListOf()
    private lateinit var context: Context

    class AlbumViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.findViewById(R.id.album_item_title)
        val artist: TextView = v.findViewById(R.id.album_item_artist)
        val albumArt: ImageView = v.findViewById(R.id.album_item_art)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        context = parent.context
        return AlbumViewHolder(view)
    }

    override fun getItemCount(): Int {
        return albumItemList.size
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            holder.title.text = albumItemList[position].album.album
            holder.artist.text = albumItemList[position].album.artist
            val albumArt = albumItemList[position].albumArt
            if(albumArt!=null){
                holder.albumArt.setImageBitmap(albumArt)
            }
        }
    }


    fun setAlbumItemList(list: MutableList<AlbumItem>) {
        albumItemList = list
//        notifyItemRangeInserted(0, list.size)
        notifyDataSetChanged()
    }
}