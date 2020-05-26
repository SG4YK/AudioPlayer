package com.github.sg4yk.audioplayer.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.extensions.isPlayEnabled
import com.github.sg4yk.audioplayer.extensions.isPlaying
import com.github.sg4yk.audioplayer.extensions.isSkipToNextEnabled
import com.github.sg4yk.audioplayer.extensions.isSkipToPreviousEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PLAYBACK_CHANNEL: String = "com.github.sg4yk.AudioPlayer.media.PLAYBACK"
const val NOW_PLAYING_NOTIFICATION: Int = 128

class NotificationBuilder(private val context: Context, private val controller: MediaControllerCompat) {
    private val notificationManager = NotificationManagerCompat.from(context)

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_white_24dp,
        context.getString(R.string.skip_to_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_arrow_white_24dp,
        context.getString(R.string.play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
    )

    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_white_24dp,
        context.getString(R.string.pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
    )

    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_white_24dp,
        context.getString(R.string.skip_to_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )

    private val stopPendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)

    suspend fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val description = controller.metadata.description

        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, PLAYBACK_CHANNEL)

        var playPauseIndex = 0
        if (playbackState.isSkipToPreviousEnabled) {
            builder.addAction(skipToPreviousAction)
            ++playPauseIndex
        }
        if (playbackState.isPlaying) {
            builder.addAction(pauseAction)
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
        }
        if (playbackState.isSkipToNextEnabled) {
            builder.addAction(skipToNextAction)
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(playPauseIndex)
            .setShowCancelButton(true)

        var albumArt: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            albumArt = MediaHunter.getThumbnail(
                context,
                description.mediaUri!!,
                300
            )
        } else {
            val audioId = description.mediaId
            if (audioId != null && audioId != "") {
                val albumId = MediaHunter.getAlbumIdFromAudioId(context, audioId.toLong())
                if (albumId != MediaHunter.ALBUM_NOT_EXIST) {
                    try {
                        val futureTarget = Glide.with(context)
                            .asBitmap()
                            .load(MediaHunter.getArtUriFromAlbumId(albumId))
                            .submit(300, 300)
                        albumArt = futureTarget.get()
                        Glide.with(context).clear(futureTarget)
                    } catch (e: Exception) {
                        Log.w("NotificationBuilder", e.message)
                    }
                }
            }
        }
//        albumArt = controller.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

        return builder.setContentIntent(controller.sessionActivity)
            .setContentText("${description.subtitle} - ${description.description}")
            .setContentTitle(description.title)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(albumArt)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_play_circle_filled_white_24dp)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun shouldCreateNowPlayingChannel(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
        notificationManager.getNotificationChannel(PLAYBACK_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        GlobalScope.launch {
            val notificationChannel = NotificationChannel(
                PLAYBACK_CHANNEL,
                "NOW_PLAYING",
                NotificationManager.IMPORTANCE_LOW
            )
                .apply {
                    description = "Empty description"
                }
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            val parcelFileDescriptor =
                context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext null
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fileDescriptor).apply {
                parcelFileDescriptor.close()
            }
        }
    }
}