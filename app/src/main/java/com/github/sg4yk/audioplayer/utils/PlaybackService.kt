package com.github.sg4yk.audioplayer.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.github.sg4yk.audioplayer.MainActivity
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.media.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*


class PlaybackService : MediaBrowserServiceCompat() {
    companion object {
        private const val SESSION_TAG = "com.sg4yk.AudioPlayer.MEDIA_SESSION"
        private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_GRID = 2
        private val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        private const val PLAYBACK_CHANNEL: String = "com.github.sg4yk.AudioPlayer.media.PLAYBACK"
        private const val PLAYBACK_NOTIFICATION_ID = 1
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationBuilder
    private lateinit var mediaSource: MetadataSource
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private var isForegroundService = false

    private val browseTree: BrowseTree by lazy {
        BrowseTree(this, mediaSource)
    }

    private val exoPlayer: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            shuffleModeEnabled = false
            playWhenReady = true
        }
    }

    override fun onCreate() {
        super.onCreate()

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), 0
        )

        mediaSession = MediaSessionCompat(this, SESSION_TAG)
            .apply {
                setSessionActivity(pendingIntent)
                isActive = true
            }



        sessionToken = mediaSession.sessionToken

        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }

        notificationBuilder = NotificationBuilder(this, mediaController)

        notificationManager = NotificationManagerCompat.from(this)

        becomingNoisyReceiver = BecomingNoisyReceiver(this, mediaSession.sessionToken)

        mediaSource = MetadataSource(this)

        serviceScope.launch() {
            mediaSource.load()
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).also { connector ->
            // Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, getString(R.string.app_name))
            )

            // Create the PlaybackPreparer of the media session connector.
            val playbackPreparer = PlaybackPreparer(
                this,
                mediaSource,
                exoPlayer,
                dataSourceFactory,
                serviceScope
            )

            connector.setPlayer(exoPlayer)
            connector.setPlaybackPreparer(playbackPreparer)
            connector.setQueueNavigator(QueueNavigator(mediaSession))
            connector.setMediaMetadataProvider(object : MediaSessionConnector.MediaMetadataProvider {
                override fun getMetadata(player: Player): MediaMetadataCompat {
                    try {
                        return player.currentTag as MediaMetadataCompat
                    } catch (e: Exception) {
                        return MediaMetadataCompat.Builder().build()
                    }
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (!PrefManager.keepBgService(this)) {
            super.onTaskRemoved(rootIntent)
            onDestroy()
        }
    }

    override fun onDestroy() {
        becomingNoisyReceiver.unregister()
        mediaSession.run {
            isActive = false
            release()
        }
        mediaSessionConnector.setPlayer(null)
        exoPlayer.run {
            stop(true)
            release()
        }
        serviceJob.cancel()
        removeNowPlayingNotification()
        stopSelf()
        super.onDestroy()
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val resultsSent = mediaSource.whenReady { successfullyInitialized ->
            if (successfullyInitialized) {
                val children =
                    browseTree[parentId]?.map { item -> MediaBrowserCompat.MediaItem(item.description, item.flag) }
                if (children != null) {
                    result.sendResult(children.toMutableList())
                } else {
                    result.sendResult(null)
                }
            } else {
                result.sendResult(null)
            }
        }
        if (!resultsSent) {
            result.detach()
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
//        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        val rootExtras = Bundle().apply {
            putBoolean(
                MEDIA_SEARCH_SUPPORTED, true
            )
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }

        return BrowserRoot(BROWSABLE_ROOT, rootExtras)
    }

    private class BecomingNoisyReceiver(
        private val context: Context,
        sessionToken: MediaSessionCompat.Token
    ) : BroadcastReceiver() {
        private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        private val controller = MediaControllerCompat(context, sessionToken)

        private var registered = false

        fun register() {
            if (!registered) {
                context.registerReceiver(this, noisyIntentFilter)
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                context.unregisterReceiver(this)
                registered = false
            }
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                controller.transportControls.pause()
            }
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        private var cachedState: Int = PlaybackStateCompat.STATE_NONE

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            val metadata = mediaController.metadata
            // Prevent duplicated update
            if (state?.state == cachedState) {
                return
            }
            cachedState = state?.state ?: PlaybackStateCompat.STATE_NONE
            serviceScope.launch {
                updateNotification(state, mediaController.metadata)
            }
            serviceScope.launch {
                delay(1000)
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val state = mediaController.playbackState
            val metadata = mediaController.metadata
            serviceScope.launch {
                updateNotification(state, metadata)
            }
        }

        private suspend fun updateNotification(state: PlaybackStateCompat?, metadata: MediaMetadataCompat) {
            val updatedState = state?.state ?: PlaybackStateCompat.STATE_NONE

            val notification =
                if (updatedState == PlaybackStateCompat.STATE_PLAYING
                    || updatedState == PlaybackStateCompat.STATE_PAUSED
                ) {
                    notificationBuilder.buildNotification(mediaSession.sessionToken, state!!, metadata)
                } else {
                    null
                }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING -> {
                    // DO NOTHING
                }

                PlaybackStateCompat.STATE_PLAYING -> {
                    becomingNoisyReceiver.register()
                    if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        if (!isForegroundService) {
                            ContextCompat.startForegroundService(
                                this@PlaybackService,
                                Intent(this@PlaybackService, PlaybackService::class.java)
                            )
                            startForeground(NOW_PLAYING_NOTIFICATION, notification)
                            isForegroundService = true
                        }
                    }
                }

                else -> {
                    becomingNoisyReceiver.unregister()
                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false

                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                        }

                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            removeNowPlayingNotification()
                        }
                    }
                }
            }
        }
    }

    private fun removeNowPlayingNotification() {
        stopForeground(true)
    }

    private inner class QueueNavigator(
        mediaSession: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            val metadata = player.currentTag as MediaMetadataCompat
            return metadata.description
        }
    }
}