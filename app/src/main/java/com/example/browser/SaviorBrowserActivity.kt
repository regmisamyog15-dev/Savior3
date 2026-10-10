package com.example.browser

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.QuickCirclePreferences
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl
import com.example.ui.theme.RoseError
import com.example.ui.theme.Sage

class SaviorBrowserActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var preferences: QuickCirclePreferences
    private lateinit var recentStore: BrowserRecentStore

    private var isInPiPModeState by mutableStateOf(false)
    private var currentUrlState by mutableStateOf("https://m.youtube.com")
    private var inputUrlState by mutableStateOf("https://m.youtube.com")
    private var pageProgressState by mutableIntStateOf(0)
    private var isPageLoadingState by mutableStateOf(false)
    private var showControlPanelState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = QuickCirclePreferences.getInstance(this)
        recentStore = BrowserRecentStore.getInstance(this)
        SaviorMediaManager.init(this)

        // Setup Media Control hooks from MediaSession / Notifications with YouTube movie_player support
        SaviorMediaManager.onPlayRequested = {
            runOnUiThread {
                executeJavaScript("""
                    var p = document.getElementById('movie_player');
                    if (p && typeof p.playVideo === 'function') {
                        p.playVideo();
                    } else {
                        var v = document.querySelector('video') || document.querySelector('audio');
                        if (v) v.play();
                    }
                """.trimIndent())
            }
        }
        SaviorMediaManager.onPauseRequested = {
            runOnUiThread {
                executeJavaScript("""
                    var p = document.getElementById('movie_player');
                    if (p && typeof p.pauseVideo === 'function') {
                        p.pauseVideo();
                    } else {
                        var v = document.querySelector('video') || document.querySelector('audio');
                        if (v) v.pause();
                    }
                """.trimIndent())
            }
        }
        SaviorMediaManager.onNextRequested = {
            runOnUiThread {
                executeJavaScript("""
                    var p = document.getElementById('movie_player');
                    if (p && typeof p.nextVideo === 'function') {
                        p.nextVideo();
                    } else {
                        var nextBtn = document.querySelector('.ytp-next-button, [aria-label="Next"], [aria-label="Next video"]');
                        if (nextBtn) nextBtn.click();
                    }
                """.trimIndent())
            }
        }
        SaviorMediaManager.onPreviousRequested = {
            runOnUiThread {
                executeJavaScript("""
                    var p = document.getElementById('movie_player');
                    if (p && typeof p.previousVideo === 'function') {
                        p.previousVideo();
                    } else {
                        var prevBtn = document.querySelector('.ytp-prev-button, [aria-label="Previous"], [aria-label="Previous video"]');
                        if (prevBtn) {
                            prevBtn.click();
                        } else {
                            var v = document.querySelector('video');
                            if (v) v.currentTime = 0;
                        }
                    }
                """.trimIndent())
            }
        }
        SaviorMediaManager.onSeekRequested = { posMs ->
            runOnUiThread {
                val sec = posMs / 1000.0
                executeJavaScript("""
                    var p = document.getElementById('movie_player');
                    if (p && typeof p.seekTo === 'function') {
                        p.seekTo($sec, true);
                    } else {
                        var v = document.querySelector('video') || document.querySelector('audio');
                        if (v) v.currentTime = $sec;
                    }
                """.trimIndent())
            }
        }

        // Enable auto-enter PiP on API 31+
        updateAutoPiPParams()

        val initialTarget = intent.getStringExtra(EXTRA_URL) ?: "https://m.youtube.com"
        currentUrlState = initialTarget
        inputUrlState = initialTarget

        setContent {
            MyApplicationTheme {
                BrowserContent()
            }
        }
    }

    @Composable
    private fun BrowserContent() {
        val mediaState by SaviorMediaManager.mediaState.collectAsState()
        val mediaTitle by SaviorMediaManager.mediaTitle.collectAsState()
        val isPlaying = mediaState == BrowserMediaState.PLAYING
        val focusManager = LocalFocusManager.current

        BackHandler(enabled = !isInPiPModeState) {
            if (customView != null) {
                hideCustomViewInternal()
            } else if (showControlPanelState) {
                showControlPanelState = false
            } else if (webView?.canGoBack() == true) {
                webView?.goBack()
            } else {
                finish()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Graphite
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Browser Top Chrome (Hidden in PiP mode)
                    if (!isInPiPModeState) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(Panel)
                        ) {
                            // URL Bar Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier.size(36.dp).testTag("btn_browser_back")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Savior",
                                        tint = Pearl,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Address input field
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(19.dp))
                                        .background(Graphite)
                                        .border(1.dp, LineBorder, RoundedCornerShape(19.dp))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Dim,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = inputUrlState,
                                        onValueChange = { inputUrlState = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = Pearl, fontSize = 13.sp),
                                        cursorBrush = SolidColor(Brass),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Uri,
                                            imeAction = ImeAction.Go
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onGo = {
                                                focusManager.clearFocus()
                                                loadUserUrl(inputUrlState)
                                            }
                                        ),
                                        modifier = Modifier.weight(1f).testTag("input_browser_url")
                                    )
                                    if (isPageLoadingState) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Brass,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { webView?.reload() },
                                            modifier = Modifier.size(24.dp).testTag("btn_browser_reload")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reload",
                                                tint = Dim,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // PiP Direct Action Button
                                IconButton(
                                    onClick = { enterPictureInPicture() },
                                    modifier = Modifier.size(36.dp).testTag("btn_top_pip")
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_action_split_screen),
                                        contentDescription = "PiP",
                                        tint = if (isPlaying) Sage else Dim,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Loading Progress Bar
                            if (isPageLoadingState && pageProgressState < 100) {
                                LinearProgressIndicator(
                                    progress = { pageProgressState / 100f },
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = Brass,
                                    trackColor = Color.Transparent
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Line))
                            }

                            // Quick Shortcuts Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShortcutChip("YouTube", "https://m.youtube.com") { loadUserUrl(it) }
                                ShortcutChip("YT Music", "https://music.youtube.com") { loadUserUrl(it) }
                                ShortcutChip("Google", "https://www.google.com") { loadUserUrl(it) }
                                ShortcutChip("Podcasts", "https://podcasts.google.com") { loadUserUrl(it) }
                            }
                        }
                    }

                    // Main Android WebView Host
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                createConfiguredWebView(ctx).also {
                                    webView = it
                                    it.loadUrl(currentUrlState)
                                }
                            },
                            update = { wv ->
                                webView = wv
                            },
                            modifier = Modifier.fillMaxSize().testTag("browser_webview")
                        )
                    }

                    // Bottom Floating Playback Bar (Hidden in PiP mode)
                    if (!isInPiPModeState && mediaState != BrowserMediaState.NONE) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            color = PanelElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showControlPanelState = true }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Sage else Dim)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = mediaTitle,
                                            color = Pearl,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Tap to open Savior Background Controls",
                                            color = Dim,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                SaviorMediaManager.onPauseRequested?.invoke()
                                            } else {
                                                SaviorMediaManager.onPlayRequested?.invoke()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp).testTag("btn_bottom_play_pause")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Brass,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { enterPictureInPicture() },
                                        modifier = Modifier.size(36.dp).testTag("btn_bottom_pip")
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_action_split_screen),
                                            contentDescription = "PiP",
                                            tint = Sage,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Bottom Media Control Panel
                AnimatedVisibility(
                    visible = showControlPanelState && !isInPiPModeState,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MediaControlPanel(
                        onDismiss = { showControlPanelState = false },
                        onEnterPiP = {
                            showControlPanelState = false
                            enterPictureInPicture()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun ShortcutChip(title: String, url: String, onSelect: (String) -> Unit) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PanelElevated)
                .clickable { onSelect(url) }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = title, color = Pearl, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(context: Context): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                setGeolocationEnabled(false)
                loadsImagesAutomatically = true
                blockNetworkImage = false
                blockNetworkLoads = false

                // Clean User-Agent matching pure Google Chrome for Android
                val defaultUa = WebSettings.getDefaultUserAgent(context)
                val sanitizedUa = defaultUa
                    .replace("; wv", "")
                    .replace("; U;", ";")
                    .replace(Regex("Version/[0-9.]+\\s*"), "")
                userAgentString = if (sanitizedUa.contains("Chrome")) {
                    sanitizedUa
                } else {
                    "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                }
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            cookieManager.flush()

            // Inject Standard JavaScript Bridge to observe HTML5 <video> and <audio> playback state
            addJavascriptInterface(SaviorJsBridge(), "SaviorBridge")

            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.w(TAG, "WebView render process gone: didCrash=${detail?.didCrash()}")
                    isPageLoadingState = false
                    view?.let { wv ->
                        wv.post {
                            try {
                                wv.loadUrl(currentUrlState)
                            } catch (_: Exception) {}
                        }
                    }
                    return true
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return handleUrlNavigation(view, url)
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false
                    return handleUrlNavigation(view, url)
                }

                private fun handleUrlNavigation(view: WebView?, url: String): Boolean {
                    // Standard HTTP/HTTPS: stay inside Savior Browser
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        return false
                    }

                    // Intercept intent:// schemes (YouTube mobile redirects to intent:// to force open native app)
                    if (url.startsWith("intent://")) {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (!fallbackUrl.isNullOrEmpty()) {
                                view?.loadUrl(fallbackUrl)
                                return true
                            }
                            val dataUri = intent.dataString
                            if (dataUri != null && (dataUri.startsWith("http://") || dataUri.startsWith("https://"))) {
                                view?.loadUrl(dataUri)
                                return true
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse intent URL: $url", e)
                        }
                        return true
                    }

                    // Block external schemes like tel:, mailto:, market:, vnd.youtube: from breaking WebView
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isPageLoadingState = true
                    url?.let {
                        currentUrlState = it
                        inputUrlState = it
                    }
                    injectEarlyMediaOverrides()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPageLoadingState = false
                    url?.let {
                        currentUrlState = it
                        inputUrlState = it
                        recentStore.addWebsiteVisit(view?.title ?: it, it)
                    }
                    injectMediaObserver()
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        isPageLoadingState = false
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    pageProgressState = newProgress
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("SaviorBrowserConsole", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                    return true
                }

                // Grant protected media and audio resources for HTML5 DRM/EME
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.let {
                        it.grant(it.resources)
                    }
                }

                override fun getDefaultVideoPoster(): Bitmap? {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        hideCustomViewInternal()
                    }
                    customView = view
                    customViewCallback = callback
                    val decor = window.decorView as? FrameLayout
                    decor?.addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }

                override fun onHideCustomView() {
                    hideCustomViewInternal()
                }
            }
        }
    }

    private fun hideCustomViewInternal() {
        customView?.let {
            val decor = window.decorView as? FrameLayout
            decor?.removeView(it)
        }
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    private fun injectEarlyMediaOverrides() {
        val js = """
            (function() {
                try {
                    if (!window._saviorProtoPatched) {
                        window._saviorProtoPatched = true;
                        try {
                            Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                            Object.defineProperty(Document.prototype, 'hidden', { get: function() { return false; }, configurable: true });
                            Object.defineProperty(Document.prototype, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                        } catch(e) {}
                        try {
                            document.hasFocus = function() { return true; };
                        } catch(e) {}
                        window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                        document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        executeJavaScript(js)
    }

    private fun injectMediaObserver() {
        val js = """
            (function() {
                try {
                    if (!window._saviorProtoPatched) {
                        window._saviorProtoPatched = true;
                        try {
                            Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                            Object.defineProperty(Document.prototype, 'hidden', { get: function() { return false; }, configurable: true });
                            Object.defineProperty(Document.prototype, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                        } catch(e) {}
                        try {
                            document.hasFocus = function() { return true; };
                        } catch(e) {}
                        window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                        document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    }
                } catch(e) {}

                // Safe Ad Auto-Skip: Only click the legitimate skip button when YouTube presents it
                if (!window._saviorAdWatchdogInstalled) {
                    window._saviorAdWatchdogInstalled = true;
                    setInterval(function() {
                        try {
                            var skip = document.querySelector('.ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, [id^="skip-button"] button, .videoAdUiSkipButton');
                            if (skip) {
                                skip.click();
                            }
                            var overlay = document.querySelector('.ytp-ad-overlay-close-button');
                            if (overlay) {
                                overlay.click();
                            }
                        } catch(err) {}
                    }, 1000);
                }

                if (window._saviorObserverInstalled) return;
                window._saviorObserverInstalled = true;

                function notifyMedia(el) {
                    try {
                        if (!el) return;
                        var isPlaying = !el.paused && !el.ended && el.readyState > 1;
                        var isBuffering = !el.paused && el.readyState <= 1;
                        var title = document.title || "";
                        var channel = "";
                        try {
                            var c = document.querySelector('.slim-owner-channel-name, ytm-channel-name, #channel-name, .ytp-title-channel-name');
                            if (c) channel = c.innerText.trim();
                        } catch(e) {}
                        var cur = Math.floor((el.currentTime || 0) * 1000);
                        var dur = Math.floor((el.duration || 0) * 1000);
                        if (isNaN(dur)) dur = 0;

                        if (window.SaviorBridge) {
                            window.SaviorBridge.onMediaStateChanged(isPlaying, isBuffering, title, channel, cur, dur);
                        }
                    } catch(err) {}
                }

                document.addEventListener('play', function(e) { notifyMedia(e.target); }, true);
                document.addEventListener('pause', function(e) { notifyMedia(e.target); }, true);
                document.addEventListener('playing', function(e) { notifyMedia(e.target); }, true);
                document.addEventListener('timeupdate', function(e) { 
                    if (e.target && !e.target.paused) notifyMedia(e.target); 
                }, true);
                document.addEventListener('ended', function(e) { notifyMedia(e.target); }, true);

                // Auto-detect existing video/audio on page
                var existing = document.querySelector('video') || document.querySelector('audio');
                if (existing) {
                    notifyMedia(existing);
                }
            })();
        """.trimIndent()
        executeJavaScript(js)
    }

    private fun executeJavaScript(script: String) {
        webView?.evaluateJavascript(script, null)
    }

    private fun loadUserUrl(raw: String) {
        var clean = raw.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = if (clean.contains(".") && !clean.contains(" ")) {
                "https://$clean"
            } else {
                "https://m.youtube.com/results?search_query=" + java.net.URLEncoder.encode(clean, "UTF-8")
            }
        }
        currentUrlState = clean
        inputUrlState = clean
        webView?.loadUrl(clean)
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                try {
                    enterPictureInPictureMode()
                } catch (fallbackEx: Exception) {
                    // PiP not supported on device
                }
            }
        }
    }

    private fun updateAutoPiPParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(preferences.autoPipOnHome.value)
                    .setAspectRatio(Rational(16, 9))
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                // Ignore if not supported
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If user presses Home while watching media, enter PiP if enabled
        if (preferences.autoPipOnHome.value && SaviorMediaManager.mediaState.value == BrowserMediaState.PLAYING) {
            enterPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPiPModeState = isInPictureInPictureMode
        SaviorMediaManager.setPiPActive(isInPictureInPictureMode)
    }

    override fun onPause() {
        super.onPause()
        // If keepAudioActive is enabled, do NOT pause WebView timers so media continues in background!
        if (!preferences.keepAudioActiveInBackground.value && !isInPiPModeState) {
            webView?.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        updateAutoPiPParams()
    }

    override fun onDestroy() {
        super.onDestroy()
        SaviorMediaManager.onPlayRequested = null
        SaviorMediaManager.onPauseRequested = null
        SaviorMediaManager.onNextRequested = null
        SaviorMediaManager.onPreviousRequested = null
        SaviorMediaManager.onSeekRequested = null

        webView?.apply {
            stopLoading()
            clearHistory()
            destroy()
        }
        webView = null
    }

    inner class SaviorJsBridge {
        @JavascriptInterface
        fun onMediaStateChanged(
            isPlaying: Boolean,
            isBuffering: Boolean,
            title: String,
            channel: String,
            currentTimeMs: Long,
            durationMs: Long
        ) {
            runOnUiThread {
                val sourceDomain = try {
                    java.net.URI(currentUrlState).host?.replace("www.", "") ?: "Savior Browser"
                } catch (e: Exception) {
                    "Savior Browser"
                }
                SaviorMediaManager.updatePlaybackState(
                    context = this@SaviorBrowserActivity,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    title = title,
                    source = if (channel.isNotBlank()) "$channel • $sourceDomain" else sourceDomain,
                    positionMs = currentTimeMs,
                    durationMs = durationMs
                )
            }
        }
    }

    companion object {
        private const val TAG = "SaviorBrowser"
        const val EXTRA_URL = "extra_target_url"

        fun launch(context: Context, url: String = "https://m.youtube.com") {
            val intent = Intent(context, SaviorBrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            context.startActivity(intent)
        }
    }
}
