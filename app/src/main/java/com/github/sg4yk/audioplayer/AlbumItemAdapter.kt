package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PrefManager


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
        holder.title.text = albumItemList[position].album.album
        holder.artist.text = albumItemList[position].album.artist

//            val albumArt = albumItemList[position].albumArt
//            if (albumArt != null) {
//                holder.albumArt.setImageBitmap(albumArt)
//            }

        Glide.with(holder.albumArt)
            .load(MediaHunter.getArtUriFromAlbumId(albumItemList[position].album.id!!))
            .thumbnail(0.25f)
            .centerInside()
            .into(holder.albumArt);


        val intent = Intent(context, AlbumDetailActivity::class.java).apply {
            putExtra(
                AlbumDetailActivity.EXTRA_TAG,
                arrayOf(
                    albumItemList[position].album.album ?: "Unknown album",
                    albumItemList[position].album.artist ?: "Unknown artist",
                    albumItemList[position].album.id.toString()
                )
            )
        }


        holder.view.setOnClickListener {
            if (!PrefManager.animationReduced(context)) {
                val options = ActivityOptionsCompat.makeClipRevealAnimation(
                    holder.view,
                    0, 0,
                    holder.view.width,
                    holder.view.height
                )
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
        }

    }


    fun setAlbumItemList(list: MutableList<AlbumItem>) {
        albumItemList = list
//        notifyItemRangeInserted(0, list.size)
        notifyDataSetChanged()
    }
}