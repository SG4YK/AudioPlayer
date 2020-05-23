package com.github.sg4yk.audioplayer

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.media.MetadataSource
import com.github.sg4yk.audioplayer.utils.MediaHunter
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.Q)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val preloadItemsCount = 15
    }

    var audioItemsLiveData: MutableLiveData<MutableList<AudioItem>> = MutableLiveData()

    var albumItemsLiveData: MutableLiveData<MutableList<AlbumItem>> = MutableLiveData()

    private var audioItems: MutableList<AudioItem> = mutableListOf()

    private var albumItems: MutableList<AlbumItem> = mutableListOf()

    private var metadataList: MutableList<MediaMetadataCompat> = mutableListOf()

    private var mediaSource = MetadataSource(application)

    private var appModelJob = SupervisorJob()

    private var context = application

    init {
        val preloadJob = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            mediaSource.load()
            mediaSource.whenReady {

                metadataList = mediaSource.toMutableList()
                metadataList.forEach {
                    audioItems.add(AudioItem(it))
                }

                // preload album art for first 15 items
                val loadCount =
                    (if (audioItems.size >= preloadItemsCount) preloadItemsCount else audioItems.size) - 1
                for (i in 0..loadCount) {
                    async {
                        audioItems[i].thumbnail =
                            MediaHunter.getThumbnail(
                                application,
                                audioItems[i].metadata.description!!.mediaUri!!,
                                AudioItem.THUMBNAIL_SIZE
                            )
                    }
                }
            }
        }

        val loadJob = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            preloadJob.join()
            // show the preloaded item
            audioItemsLiveData.postValue(
                audioItems
            )
            // load the rest part
            for (i in preloadItemsCount..audioItems.size - 1) {
                async {
                    try {
                        audioItems[i].thumbnail =
                            MediaHunter.getThumbnail(
                                application,
                                audioItems[i].metadata.description!!.mediaUri!!,
                                AudioItem.THUMBNAIL_SIZE
                            )
                    } catch (e: Exception) {
                        Log.w("AppViewModel", e.message)
                    }
                }
            }
        }

        GlobalScope.launch {
            loadJob.join()
            // all items loaded
            audioItemsLiveData.postValue(
                audioItems
            )
        }

        refreshAlbumItems()

    }

    fun refreshAudioItems() {
        val job = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            mediaSource.load()
            mediaSource.whenReady {
                metadataList = mediaSource.toMutableList()
                audioItems.clear()
                metadataList.forEach {
                    audioItems.add(AudioItem(it))
                }
                audioItems.forEach {
                    async {
                        it.thumbnail = MediaHunter.getThumbnail(
                            context,
                            it.metadata.description!!.mediaUri!!,
                            AudioItem.THUMBNAIL_SIZE
                        )
                    }
                }
            }
        }
        GlobalScope.launch {
            job.join()
            audioItemsLiveData.postValue(audioItems)
        }
    }

    fun refreshAlbumItems() {
        albumItems.clear()
        var loadAlbum = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            val albumList = MediaHunter.getAllAlbums(context).toMutableList()
            albumList.forEach {
                albumItems.add(AlbumItem(it))
            }
            albumItems.forEach {
                async {
                    it.albumArt = MediaHunter.getThumbnail(
                        context,
                        it.album.uri.toString(),
                        AlbumItem.ALBUM_ART_SIZE
                    )
                }
            }
        }
        GlobalScope.launch {
            loadAlbum.join()
            albumItemsLiveData.postValue(albumItems)
        }
    }
}



class AudioItem(val metadata: MediaMetadataCompat) {
    companion object {
        const val THUMBNAIL_SIZE = 80
    }
    var thumbnail: Bitmap? = null
}

class AlbumItem(val album: Album) {
    companion object {
        const val ALBUM_ART_SIZE = 250
    }
    var albumArt: Bitmap? = null
}