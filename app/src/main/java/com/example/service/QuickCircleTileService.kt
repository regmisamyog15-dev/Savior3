package com.example.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.data.QuickCirclePreferences

class QuickCircleTileService : TileService() {

    private lateinit var preferences: QuickCirclePreferences

    override fun onCreate() {
        super.onCreate()
        preferences = QuickCirclePreferences.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val newState = !preferences.isEnabled.value
        preferences.setEnabled(newState)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isEnabled = preferences.isEnabled.value
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Savior"
        tile.subtitle = if (isEnabled) "Active" else "Off"
        tile.updateTile()
    }
}
