package com.example.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SaviorMediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("SaviorMediaReceiver", "onReceive: $action")

        when (action) {
            SaviorMediaManager.ACTION_PLAY -> {
                SaviorMediaManager.onPlayRequested?.invoke()
            }
            SaviorMediaManager.ACTION_PAUSE -> {
                SaviorMediaManager.onPauseRequested?.invoke()
            }
            SaviorMediaManager.ACTION_NEXT -> {
                SaviorMediaManager.onNextRequested?.invoke()
            }
            SaviorMediaManager.ACTION_PREV -> {
                SaviorMediaManager.onPreviousRequested?.invoke()
            }
        }
    }
}
