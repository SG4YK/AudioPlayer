package com.github.sg4yk.audioplayer.ui.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.playback.PlaybackManager
import com.github.sg4yk.audioplayer.utils.Generic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

open class AudioItemAdapter : RecyclerView.Adapter<AudioItemAdapter.AudioViewHolder>() {
    protected var audioItems: MutableList<Audio> = mutableListOf()
    protected var selectedItems: MutableList<Boolean> = mutableListOf()
    private var selectedCount = 0

    fun getSelectedCount() = selectedCount

    fun selectedAudioItems(): List<Audio> {
        return audioItems.filterIndexed { index, audio -> selectedItems[index] }
    }

    fun select(index: Int, select: Boolean) {
        try {
            selectedItems[index] = select
            if (select) {
                selectedCount++
            } else {
                selectedCount--
            }
            notifyItemChanged(index)
        } catch (e: Exception) {
            Log.d("AudioItemAdapter", e.message)
        }
    }

    open fun clearSelection() {
        for (i in selectedItems.indices) {
            if (selectedItems[i]) {
                selectedItems[i] = false
                selectedCount--
                notifyItemChanged(i)
            }
        }
    }

    open fun selectAll() {
        for (i in selectedItems.indices) {
            if (!selectedItems[i]) {
                selectedItems[i] = true
                selectedCount++
                notifyItemChanged(i)
            }
        }
    }

    protected lateinit var context: Context

    inner class AudioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view = v
        val initialBackground: Drawable = v.background
        val title: TextView = v.findViewById(R.id.audioItemTitle)
        val description: TextView = v.findViewById(R.id.audioItemDescription)
        val albumArt: ImageView = v.findViewById(R.id.audioItemAlbumArt)
        val duration: TextView = v.findViewById(R.id.duration)

        fun select(select: Boolean) {
            if (select) {
                view.background = context.getDrawable(R.drawable.md_item_selected)
            } else {
                view.background = initialBackground
            }
        }
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
            Log.d("MultiSelect", "BindingHolder ${audio.title} ${selectedItems[position]}")
            holder.select(selectedItems[position])
        }
    }

    fun setAudioItemList(list: MutableList<Audio>) {
        audioItems = list
        selectedItems = MutableList(audioItems.size) { false }
        notifyDataSetChanged()
    }
}