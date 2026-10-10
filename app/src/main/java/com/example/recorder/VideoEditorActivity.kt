package com.example.recorder

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl
import com.example.ui.theme.RoseError
import com.example.ui.theme.Sage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.Locale

class VideoEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val videoUri = intent.getParcelableExtra<Uri>(EXTRA_VIDEO_URI)
        if (videoUri == null) {
            Toast.makeText(this, "No video provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            VideoEditorScreen(
                initialVideoUri = videoUri,
                onClose = { finish() },
                onShare = { uri -> shareVideo(uri) },
                onSaveToGallery = { file -> saveToPublicGallery(file) }
            )
        }
    }

    private fun shareVideo(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Recording via"))
    }

    private fun saveToPublicGallery(file: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Savior")
                }
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(file).use { input -> input.copyTo(out) }
                    }
                    Toast.makeText(this, "Saved to Movies/Savior", Toast.LENGTH_SHORT).show()
                    true
                } else false
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Savior")
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(target).use { out -> input.copyTo(out) }
                }
                Toast.makeText(this, "Saved to Movies/Savior", Toast.LENGTH_SHORT).show()
                true
            }
        } catch (e: Exception) {
            Log.e("VideoEditor", "saveToPublicGallery failed", e)
            Toast.makeText(this, "Failed saving to gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    companion object {
        const val EXTRA_VIDEO_URI = "extra_video_uri"

        fun launch(context: Context, uri: Uri) {
            val intent = Intent(context, VideoEditorActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URI, uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun VideoEditorScreen(
    initialVideoUri: Uri,
    onClose: () -> Unit,
    onShare: (Uri) -> Unit,
    onSaveToGallery: (File) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUri by remember { mutableStateOf(initialVideoUri) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var totalDurationMs by remember { mutableIntStateOf(1000) }
    var currentPositionMs by remember { mutableIntStateOf(0) }

    // Trimming range in milliseconds
    var trimStartMs by remember { mutableFloatStateOf(0f) }
    var trimEndMs by remember { mutableFloatStateOf(1000f) }
    var isTrimming by remember { mutableStateOf(false) }

    // Retrieve video duration via retriever
    LaunchedEffect(currentUri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, currentUri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val d = durStr?.toIntOrNull() ?: 1000
                totalDurationMs = d
                trimStartMs = 0f
                trimEndMs = d.toFloat()
                retriever.release()
            } catch (e: Exception) {
                Log.w("VideoEditor", "Could not query video duration", e)
            }
        }
    }

    // Playback ticker
    DisposableEffect(videoViewRef, isPlaying) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                videoViewRef?.let { vv ->
                    if (vv.isPlaying) {
                        currentPositionMs = vv.currentPosition
                        if (currentPositionMs >= trimEndMs.toInt()) {
                            vv.pause()
                            vv.seekTo(trimStartMs.toInt())
                            isPlaying = false
                        }
                    }
                }
                if (isPlaying) {
                    handler.postDelayed(this, 150L)
                }
            }
        }
        if (isPlaying) {
            handler.post(runnable)
        }
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Graphite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Pearl)
                    }
                    Text(
                        text = "Recording Editor",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pearl,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShare(currentUri) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Video", tint = Brass)
                    }
                    IconButton(
                        onClick = {
                            val file = getFileFromUri(context, currentUri)
                            if (file != null) {
                                onSaveToGallery(file)
                            }
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_action_quick_settings), contentDescription = "Save to Gallery", tint = Pearl)
                    }
                }
            }

            // Video Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setVideoURI(currentUri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = false
                                totalDurationMs = duration
                                if (trimEndMs <= 1000f) {
                                    trimEndMs = duration.toFloat()
                                }
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                seekTo(trimStartMs.toInt())
                            }
                            videoViewRef = this
                        }
                    },
                    update = { vv ->
                        // No-op
                    }
                )

                // Play / Pause Overlay Button
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xA014161A))
                            .border(1.dp, Brass, CircleShape)
                            .clickable {
                                videoViewRef?.let { vv ->
                                    if (vv.currentPosition < trimStartMs.toInt() || vv.currentPosition >= trimEndMs.toInt()) {
                                        vv.seekTo(trimStartMs.toInt())
                                    }
                                    vv.start()
                                    isPlaying = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Pearl,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    // Tap area to pause
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                videoViewRef?.pause()
                                isPlaying = false
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Scrubber & Time
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Slider(
                    value = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f,
                    onValueChange = { frac ->
                        val targetMs = (frac * totalDurationMs).toInt()
                        currentPositionMs = targetMs
                        videoViewRef?.seekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Brass,
                        activeTrackColor = Brass,
                        inactiveTrackColor = Line
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("video_scrubber")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = Pearl,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTime(totalDurationMs),
                        color = Dim,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trimming Control Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PanelElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_crop),
                                contentDescription = null,
                                tint = Brass,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trim Video",
                                color = Pearl,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Duration of trimmed clip
                        val trimmedSec = ((trimEndMs - trimStartMs) / 1000f).coerceAtLeast(0.1f)
                        Text(
                            text = "Length: ${String.format(Locale.getDefault(), "%.1fs", trimmedSec)}",
                            color = Sage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Start & End Sliders
                    Column {
                        // Start Trim Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Start: ${formatTime(trimStartMs.toInt())}", color = Pearl, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Slider(
                                value = if (totalDurationMs > 0) trimStartMs / totalDurationMs.toFloat() else 0f,
                                onValueChange = { frac ->
                                    val candidate = frac * totalDurationMs
                                    if (candidate < trimEndMs - 500f) {
                                        trimStartMs = candidate
                                        videoViewRef?.seekTo(candidate.toInt())
                                    }
                                },
                                colors = SliderDefaults.colors(thumbColor = Sage, activeTrackColor = Sage, inactiveTrackColor = Line),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // End Trim Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("End: ${formatTime(trimEndMs.toInt())}", color = Pearl, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Slider(
                                value = if (totalDurationMs > 0) trimEndMs / totalDurationMs.toFloat() else 1f,
                                onValueChange = { frac ->
                                    val candidate = frac * totalDurationMs
                                    if (candidate > trimStartMs + 500f) {
                                        trimEndMs = candidate
                                        videoViewRef?.seekTo(candidate.toInt())
                                    }
                                },
                                colors = SliderDefaults.colors(thumbColor = RoseError, activeTrackColor = RoseError, inactiveTrackColor = Line),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trim & Save Action Button
                    Button(
                        onClick = {
                            scope.launch {
                                isTrimming = true
                                val srcFile = getFileFromUri(context, currentUri)
                                if (srcFile != null) {
                                    val outFile = File(
                                        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                                        "Trimmed_${System.currentTimeMillis()}.mp4"
                                    )
                                    val ok = trimVideoLossless(srcFile, outFile, trimStartMs.toLong() * 1000L, trimEndMs.toLong() * 1000L)
                                    if (ok) {
                                        val newUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                                        currentUri = newUri
                                        videoViewRef?.setVideoURI(newUri)
                                        Toast.makeText(context, "✂️ Video trimmed successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Trimming failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isTrimming = false
                            }
                        },
                        enabled = !isTrimming,
                        colors = ButtonDefaults.buttonColors(containerColor = Brass, contentColor = Graphite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_trim_video")
                    ) {
                        if (isTrimming) {
                            CircularProgressIndicator(color = Graphite, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trimming...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(painter = painterResource(R.drawable.ic_action_crop), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trim & Save Clip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val sec = (ms / 1000)
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

private fun getFileFromUri(context: Context, uri: Uri): File? {
    if (uri.scheme == "file") {
        return uri.path?.let { File(it) }
    }
    // Search in movies directory
    val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    if (moviesDir != null) {
        val files = moviesDir.listFiles()
        val lastSegment = uri.lastPathSegment ?: ""
        val match = files?.firstOrNull { it.name.contains(lastSegment) || lastSegment.contains(it.name) }
        if (match != null) return match
        // Or pick latest file
        return files?.maxByOrNull { it.lastModified() }
    }
    return null
}

/**
 * Fast, loss-free trimming using MediaExtractor and MediaMuxer without re-encoding!
 */
private suspend fun trimVideoLossless(
    sourceFile: File,
    outputFile: File,
    startUs: Long,
    endUs: Long
): Boolean = withContext(Dispatchers.IO) {
    var extractor: MediaExtractor? = null
    var muxer: MediaMuxer? = null
    try {
        extractor = MediaExtractor()
        extractor.setDataSource(sourceFile.absolutePath)

        val trackCount = extractor.trackCount
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val indexMap = HashMap<Int, Int>(trackCount)
        var bufferSize = 1024 * 1024

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                val dstIndex = muxer.addTrack(format)
                indexMap[i] = dstIndex
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (newSize > bufferSize) bufferSize = newSize
                }
            }
        }

        muxer.start()

        // Seek extractor to sync sample closest to start
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val trackIndex = extractor.sampleTrackIndex
            if (trackIndex < 0) break

            val dstTrack = indexMap[trackIndex] ?: run {
                extractor.advance()
                return@run null
            }
            if (dstTrack == null) continue

            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            val sampleTime = extractor.sampleTime
            if (sampleTime > endUs) break

            bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
            bufferInfo.flags = extractor.sampleFlags

            muxer.writeSampleData(dstTrack, buffer, bufferInfo)
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()
        true
    } catch (e: Exception) {
        Log.e("VideoEditor", "Lossless trim error", e)
        try { muxer?.release() } catch (_: Exception) {}
        try { extractor?.release() } catch (_: Exception) {}
        false
    }
}
