package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuickCirclePreferences
import com.example.model.DockEdge
import com.example.model.QuickActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SaviorUnitTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Savior", appName)
    }

    @Test
    fun testPreferencesDefaultsAndUpdates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        QuickCirclePreferences.resetForTesting(context)
        val prefs = QuickCirclePreferences.getInstance(context)

        assertTrue(prefs.isEnabled.value)
        assertTrue(prefs.isPermanent.value)
        assertEquals(52, prefs.bubbleSizeDp.value)

        prefs.setEnabled(false)
        assertEquals(false, prefs.isEnabled.value)

        prefs.setBubbleSizeDp(60)
        assertEquals(60, prefs.bubbleSizeDp.value)

        prefs.setDockEdge(DockEdge.RIGHT)
        assertEquals(DockEdge.RIGHT, prefs.dockEdge.value)
    }

    @Test
    fun testMainActivityLaunches() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        org.junit.Assert.assertNotNull(controller.get())
    }

    @Test
    fun testQuickActionTypeResolution() {
        val volumeAction = QuickActionType.fromId("volume")
        assertEquals(QuickActionType.VOLUME, volumeAction)

        val screenshotAction = QuickActionType.fromId("screenshot")
        assertEquals(QuickActionType.SCREENSHOT, screenshotAction)
    }

    @Test
    fun testDrawablesInflate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
        androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        for (action in QuickActionType.values()) {
            val d = androidx.core.content.ContextCompat.getDrawable(context, action.iconRes)
            org.junit.Assert.assertNotNull(d)
        }
    }

    @Test
    fun testPiPDiagnosticPlaybackStateMapping() {
        assertEquals("STATE_PLAYING", com.example.service.PiPDiagnosticManager.playbackStateToString(android.media.session.PlaybackState.STATE_PLAYING))
        assertEquals("STATE_PAUSED", com.example.service.PiPDiagnosticManager.playbackStateToString(android.media.session.PlaybackState.STATE_PAUSED))
        assertEquals("STATE_NONE", com.example.service.PiPDiagnosticManager.playbackStateToString(android.media.session.PlaybackState.STATE_NONE))
    }

    @Test
    fun testPiPDiagnosticRecordCreation() {
        val record = com.example.service.PiPAttemptRecord(
            attemptNumber = 1,
            timestamp = "12:00:00.000",
            youtubeStateBefore = "STATE_PLAYING",
            videoTitle = "Test Video",
            homeDispatched = true,
            pipObserved = true,
            timingMs = 3050L,
            finalResult = com.example.service.PiPResult.PIP_OBSERVED,
            rawTimeline = "BG_PLAY_START\n..."
        )
        assertEquals(1, record.attemptNumber)
        assertTrue(record.homeDispatched)
        assertTrue(record.pipObserved)
        assertEquals(com.example.service.PiPResult.PIP_OBSERVED, record.finalResult)
    }

    @Test
    fun testBrowserRecentStore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.browser.BrowserRecentStore.getInstance(context)
        store.addWebsiteVisit("YouTube", "https://m.youtube.com")
        val items = store.recentItems.value
        assertTrue(items.isNotEmpty())
        assertEquals("YouTube", items[0].title)
    }

    @Test
    fun testSaviorBrowserMediaState() {
        assertEquals("Playing", com.example.browser.BrowserMediaState.PLAYING.label)
        assertEquals("Paused", com.example.browser.BrowserMediaState.PAUSED.label)
        assertEquals("No Media", com.example.browser.BrowserMediaState.NONE.label)
    }
}
