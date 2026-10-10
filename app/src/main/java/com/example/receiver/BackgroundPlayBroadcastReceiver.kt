package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.BackgroundPlayStateMachine

class BackgroundPlayBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        when (action) {
            ACTION_RESUME_YOUTUBE -> {
                BackgroundPlayStateMachine.onUserRequestedResume(context)
            }
            ACTION_DISMISS_NOTIFICATION -> {
                BackgroundPlayStateMachine.dismissNotification(context)
            }
        }
    }

    companion object {
        private const val TAG = "BgPlayReceiver"
        const val ACTION_RESUME_YOUTUBE = "com.example.action.RESUME_YOUTUBE"
        const val ACTION_DISMISS_NOTIFICATION = "com.example.action.DISMISS_NOTIFICATION"
    }
}
