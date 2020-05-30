package com.github.sg4yk.audioplayer

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.utils.MediaHunter
import kotlinx.android.synthetic.main.album_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class AlbumItemAdapter : RecyclerView.Adapter<AlbumItemAdapter.AlbumViewHolder>() {
    protected var albumList: MutableList<Album> = mutableListOf()
    private lateinit var context: Context
    private lateinit var imgLoader: RequestManager

    class AlbumViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.title
        val artist: TextView = v.description
        val albumArt: ImageView = v.albumArt
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        context = parent.context
        imgLoader = Glide.with(context)
        return AlbumViewHolder(view)
    }

    override fun getItemCount(): Int = albumList.size

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.title.text = albumList[position].album
        holder.artist.text = albumList[position].artist
        try {
            GlobalScope.launch(Dispatchers.Main) {
                val uri = MediaHunter.getArtUriFromAlbumId(albumList[position].id)
                Glide.with(holder.view)
                    .load(uri)
                    .thumbnail(0.25f)
                    .centerInside()
                    .placeholder(R.drawable.default_album_art_blue)
                    .into(holder.albumArt)
            }
        } catch (e: Exception) {
            Log.w("AlbumItemAdapter", e.message)
        }

        holder.view.setOnClickListener {
            AlbumDetailActivity.start(
                context,
                albumList[position],
                holder.view,
                holder.albumArt.drawable.toBitmap(100, 100)
            )
        }
    }

    fun setAlbums(list: MutableList<Album>) {
        albumList = list
        notifyDataSetChanged()
    }
}