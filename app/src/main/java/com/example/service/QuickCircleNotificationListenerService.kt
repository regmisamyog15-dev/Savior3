package com.example.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

data class ActiveMediaInfo(
    val packageName: String,
    val playbackState: Int,
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val sessionToken: MediaSession.Token?
)

class QuickCircleNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    companion object {
        var isConnected: Boolean = false
            private set

        fun getActiveControllers(context: Context): List<MediaController> {
            return try {
                val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val component = ComponentName(context, QuickCircleNotificationListenerService::class.java)
                manager?.getActiveSessions(component) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun getPlayingMediaInfo(context: Context): ActiveMediaInfo? {
            val controllers = getActiveControllers(context)
            val target = controllers.firstOrNull { controller ->
                val state = controller.playbackState?.state ?: PlaybackState.STATE_NONE
                state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_CONNECTING ||
                state == PlaybackState.STATE_FAST_FORWARDING ||
                state == PlaybackState.STATE_REWINDING
            } ?: controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PAUSED
            } ?: return null

            val pState = target.playbackState?.state ?: PlaybackState.STATE_NONE
            val metadata = target.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val isPlaying = pState == PlaybackState.STATE_PLAYING || pState == PlaybackState.STATE_BUFFERING

            return ActiveMediaInfo(
                packageName = target.packageName,
                playbackState = pState,
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                sessionToken = target.sessionToken
            )
        }
    }
}
