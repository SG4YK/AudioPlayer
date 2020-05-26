package com.github.sg4yk.audioplayer

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AudioItemAdapter() : RecyclerView.Adapter<AudioItemAdapter.AudioViewHolder>() {
    private var audioItems: MutableList<Audio> = mutableListOf()
    private lateinit var context: Context

    class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.findViewById(R.id.audioItemTitle)
        val description: TextView = v.findViewById(R.id.audioItemDescription)
        val albumArt: ImageView = v.findViewById(R.id.audioItemAlbumArt)
        val duration: TextView = v.findViewById(R.id.duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.audio_item, parent, false)
        context = parent.context
        return AudioViewHolder(view)
    }

    override fun getItemCount(): Int {
        return audioItems.size
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            val audio = audioItems[position]
            async {
                holder.title.text = audio.title
                holder.description.text = "${audio.artist ?: "Unknown artist"} - ${audio.album ?: "Unknown album"}"
                holder.duration.text = Generic.msecToStr(audioItems[position].duration ?: 0)
                holder.view.setOnClickListener {
                    PlaybackManager.playAudioFromId(audio.id.toString())
                }
            }

            async {
                try {
                    Glide.with(holder.albumArt)
                        .load(MediaHunter.getArtUriFromAlbumId(audio.albumId ?: -1))
                        .placeholder(R.drawable.default_album_art_blue)
                        .thumbnail(0.25f)
                        .centerInside()
                        .into(holder.albumArt)
                } catch (e: Exception) {
                    Log.w("AudioItemAdapter", e.message)
                }
            }
        }
    }

    fun setAudioItemList(list: MutableList<Audio>) {
        audioItems = list
        notifyDataSetChanged()
    }
}