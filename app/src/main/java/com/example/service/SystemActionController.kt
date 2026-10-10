package com.example.service

import com.example.model.QuickActionType

interface SystemActionController {
    fun performAction(
        action: QuickActionType,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit = { _, _, _ -> }
    )
    fun getVolumeInfo(): Triple<Int, Int, Boolean>
    fun setVolume(
        newVol: Int,
        playSound: Boolean,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit = { _, _, _ -> }
    )
    fun toggleMute(callback: (current: Int, max: Int, isMuted: Boolean) -> Unit = { _, _, _ -> })
}
