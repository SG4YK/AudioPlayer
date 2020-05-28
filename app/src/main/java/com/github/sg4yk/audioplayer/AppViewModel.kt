package com.github.sg4yk.audioplayer

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.sg4yk.audioplayer.media.*
import com.github.sg4yk.audioplayer.utils.MediaHunter
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.Q)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val preloadItemsCount = 15
    }

    var audioItemsLiveData: MutableLiveData<MutableList<Audio>> = MutableLiveData()

    var albumItemsLiveData: MutableLiveData<MutableList<Album>> = MutableLiveData()

    var artistItemsLiveData: MutableLiveData<MutableList<Artist>> = MutableLiveData()

    var playlistItemsLiveData: MutableLiveData<MutableList<Playlist>> = MutableLiveData()

    private var audioItems: MutableList<Audio> = mutableListOf()

    private var albumItems: MutableList<Album> = mutableListOf()

    private var artistItems: MutableList<Artist> = mutableListOf()

    private var playlistItems: MutableList<Playlist> = mutableListOf()

    private var metadataList: MutableList<MediaMetadataCompat> = mutableListOf()

    private var mediaSource = MetadataSource(application)

    private var appModelJob = SupervisorJob()

    private var context = application

    init {
        Log.d("AppViewModel", "init")
        GlobalScope.launch {
            delay(100)
            refreshAudioItems()

            delay(300)
            refreshAlbumItems()

            delay(300)
            refreshArtistItems()

            delay(300)
            refreshPlaylistItems()
        }
    }

    fun refreshAll() {
        GlobalScope.launch {
            refreshAudioItems()
            refreshAlbumItems()
            refreshArtistItems()
            refreshPlaylistItems()
        }
    }

    fun refreshAudioItems() {
        audioItems.clear()
        val job = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            audioItems = MediaHunter.getAllAudio(context)
        }
        GlobalScope.launch {
            job.join()
            audioItemsLiveData.postValue(audioItems)
        }
    }

    fun refreshAlbumItems() {
        albumItems.clear()
        val job = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            albumItems = MediaHunter.getAllAlbums(context)
        }
        GlobalScope.launch {
            job.join()
            albumItemsLiveData.postValue(albumItems)
        }
    }

    fun refreshArtistItems() {
        artistItems.clear()
        val job = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            artistItems = MediaHunter.getAllArtists(context)
        }
        GlobalScope.launch {
            job.join()
            artistItemsLiveData.postValue(artistItems)
        }
    }

    fun refreshPlaylistItems() {
        playlistItems.clear()
        val job = GlobalScope.launch(Dispatchers.IO + appModelJob) {
            playlistItems = MediaHunter.getAllPlaylists(context)
        }
        GlobalScope.launch {
            job.join()
            playlistItemsLiveData.postValue(playlistItems)
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