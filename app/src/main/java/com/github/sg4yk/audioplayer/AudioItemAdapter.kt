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

open class AudioItemAdapter : RecyclerView.Adapter<AudioItemAdapter.AudioViewHolder>() {
    protected var audioItems: MutableList<Audio> = mutableListOf()
    protected lateinit var context: Context

    class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.findViewById(R.id.audioItemTitle)
        val description: TextView = v.findViewById(R.id.audioItemDescription)
        val albumArt: ImageView = v.findViewById(R.id.audioItemAlbumArt)
        val duration: TextView = v.findViewById(R.id.duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.audio_item, parent, false)
        return AudioViewHolder(view)
    }

    override fun getItemCount(): Int = audioItems.size

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            val audio = audioItems[position]
            async {
                holder.title.text = audio.title
                holder.description.text = "${audio.artist ?: "Unknown artist"} - ${audio.album ?: "Unknown album"}"
                holder.duration.text = Generic.msecToStr(audioItems[position].duration ?: 0)
                holder.view.setOnClickListener {
                    PlaybackManager.loadAllAndSkipTo(audio.id.toString())
                }
            }
        }
    }

    fun setAudioItemList(list: MutableList<Audio>) {
        audioItems = list
        notifyDataSetChanged()
    }
}