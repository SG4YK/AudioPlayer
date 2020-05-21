package com.github.sg4yk.audioplayer

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.sg4yk.audioplayer.media.MetadataSource
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    var audioItemsLiveData: MutableLiveData<MutableList<AudioItem>> = MutableLiveData()

    private var audioItems: MutableList<AudioItem> = mutableListOf()

    private var metadataList: MutableList<MediaMetadataCompat> = mutableListOf()

    private var mediaSource = MetadataSource(application)

    init {
        val job = GlobalScope.launch {
            mediaSource.load()
            mediaSource.whenReady {

                metadataList = mediaSource.toMutableList()
                metadataList.forEach {
                    audioItems.add(AudioItem(it))
                }

                // load metadata
                audioItems.forEach {
                    async {
                        it.thumbnail =
                            MediaHunter.getThumbnail(
                                application,
                                it.metadata.description!!.mediaUri!!,
                                AudioItem.THUMBNAIL_SIZE
                            )
                    }
                }
            }
        }
        GlobalScope.launch {
            // wait for all thumbnail loaded
            job.join()
            audioItemsLiveData.postValue(
                audioItems
            )
        }
    }
}

class AudioItem(val metadata: MediaMetadataCompat) {
    companion object {
        const val THUMBNAIL_SIZE = 64
        const val ALBUMART_SIZE = 300
    }

    var thumbnail: Bitmap? = null
    var albumArt: Bitmap? = null
}