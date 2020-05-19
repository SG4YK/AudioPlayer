package com.github.sg4yk.audioplayer

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.utils.AudioHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager

class AudioItemAdapter() : RecyclerView.Adapter<AudioItemAdapter.AudioViewHolder>() {
    private var audioList: List<MediaMetadataCompat> = listOf()
    private lateinit var context: Context

    class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val title: TextView = v.findViewById(R.id.audio_item_title)
        val description: TextView = v.findViewById(R.id.audio_item_description)
        val albumArt: ImageView = v.findViewById(R.id.audio_item_albumart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.audio_item, parent, false)
        context = parent.context
        return AudioViewHolder(view)
    }

    override fun getItemCount(): Int {
        return audioList.size
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val metadata = audioList[position]
        val mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toString()

        holder.albumArt.post {
            holder.albumArt.setImageBitmap(AudioHunter.getAlbumArt(context, mediaId, 100))
        }

        holder.title.text = metadata.description.title

        holder.description.text = "${metadata.description.subtitle} - ${metadata.description.description}"

        holder.view.setOnClickListener {
            PlaybackManager.playAudioFromId(mediaId)
        }


    }

    fun setAudioList(list: List<MediaMetadataCompat>) {
        audioList = list
        notifyDataSetChanged()
    }
}