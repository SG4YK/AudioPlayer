package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PrefManager
import kotlinx.android.synthetic.main.album_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlbumItemAdapter : RecyclerView.Adapter<AlbumItemAdapter.AlbumViewHolder>() {
    private var albumList: MutableList<Album> = mutableListOf()
    private lateinit var context: Context
    private lateinit var imgLoader: RequestManager

    class AlbumViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.title
        val artist: TextView = v.artist
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
            Glide.with(holder.view)
                .load(MediaHunter.getArtUriFromAlbumId(albumList[position].id))
                .thumbnail(0.25f)
                .centerInside()
                .placeholder(R.drawable.default_album_art_blue)
                .into(holder.albumArt)
        } catch (e: Exception) {
            Log.w("AlbumItemAdapter", e.message)
        }

        holder.view.setOnClickListener {
            Palette.from(holder.albumArt.drawable.toBitmap(100, 100))
                .generate { palette ->
                    GlobalScope.launch {
                        val primaryColor = palette?.getDominantColor(Color.WHITE) ?: Color.WHITE
                        Log.d("HolderOnclickListener", "%x".format(primaryColor))
                        val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                            putExtra(
                                AlbumDetailActivity.METADATA_TAG,
                                arrayOf(
                                    albumList[position].album ?: "Unknown album",
                                    albumList[position].artist ?: "Unknown artist",
                                    albumList[position].id.toString()
                                )
                            )
                            putExtra(
                                AlbumDetailActivity.IS_DARK_ALBUM_ART_TAG,
                                Generic.luminance(primaryColor) < AlbumDetailActivity.STATUS_BAR_LUMINANCE_THRESHOLD
                            )
                        }

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

        }
    }

    fun setAlbums(list: MutableList<Album>) {
        albumList = list
        notifyDataSetChanged()
    }
}