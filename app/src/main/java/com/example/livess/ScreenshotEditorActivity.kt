package com.example.livess

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ScreenshotEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        val bitmap = loadBitmapFromUri(imageUri)

        if (bitmap == null) {
            Toast.makeText(this, "Failed to load screenshot image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ScreenshotEditorScreen(
                originalBitmap = bitmap,
                imageUri = imageUri,
                onClose = { finish() },
                onSave = { editedBitmap ->
                    val savedUri = saveBitmapToGallery(editedBitmap)
                    if (savedUri != null) {
                        Toast.makeText(this, "Screenshot saved to Photos/Gallery", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = { editedBitmap ->
                    val savedUri = saveBitmapToTemp(editedBitmap)
                    if (savedUri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, savedUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share edited screenshot"))
                    }
                }
            )
        }
    }

    private fun loadBitmapFromUri(uri: Uri?): Bitmap? {
        if (uri == null) return null
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap): Uri? {
        val filename = "Savior_${System.currentTimeMillis()}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Savior")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            uri
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Savior")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        }
    }

    private fun saveBitmapToTemp(bitmap: Bitmap): Uri? {
        val file = File(cacheDir, "shared_screenshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"

        fun launch(context: Context, uri: Uri) {
            val intent = Intent(context, ScreenshotEditorActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

@Composable
fun ScreenshotEditorScreen(
    originalBitmap: Bitmap,
    imageUri: Uri?,
    onClose: () -> Unit,
    onSave: (Bitmap) -> Unit,
    onShare: (Bitmap) -> Unit
) {
    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Brass) }
    var isHighlighter by remember { mutableStateOf(false) }
    var strokeSize by remember { mutableStateOf(8f) }

    val colors = listOf(
        Brass,
        Color(0xFFE06C75), // Coral Red
        Color(0xFF98C379), // Sage Green
        Color(0xFF61AFEF), // Sky Blue
        Color(0xFFE5C07B), // Gold
        Color(0xFFFFFFFF), // White
        Color(0xFF101214)  // Dark
    )

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
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Pearl)
                    }
                    Text(
                        text = "Screenshot Editor",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pearl,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Undo
                    IconButton(
                        onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1) },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_recents),
                            contentDescription = "Undo",
                            tint = if (strokes.isNotEmpty()) Pearl else Dim
                        )
                    }

                    // Clear
                    IconButton(
                        onClick = { strokes.clear() },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear All",
                            tint = if (strokes.isNotEmpty()) RoseError else Dim
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Share
                    IconButton(
                        onClick = {
                            val composite = renderCompositeBitmap(originalBitmap, strokes)
                            onShare(composite)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Brass)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val composite = renderCompositeBitmap(originalBitmap, strokes)
                            onSave(composite)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Brass, contentColor = Graphite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(start = 4.dp).testTag("btn_save_screenshot")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Central Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()
                    val imgBitmap = remember(originalBitmap) { originalBitmap.asImageBitmap() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedColor, isHighlighter, strokeSize) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            strokes.add(
                                                DrawnStroke(
                                                    points = currentPoints,
                                                    color = selectedColor,
                                                    strokeWidth = strokeSize,
                                                    isHighlighter = isHighlighter
                                                )
                                            )
                                            currentPoints = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        currentPoints = emptyList()
                                    }
                                )
                            }
                    ) {
                        // Draw screenshot background fitting within canvas
                        val scale = minOf(
                            size.width / originalBitmap.width.toFloat(),
                            size.height / originalBitmap.height.toFloat()
                        )
                        val drawW = originalBitmap.width * scale
                        val drawH = originalBitmap.height * scale
                        val left = (size.width - drawW) / 2f
                        val top = (size.height - drawH) / 2f

                        drawImage(
                            image = imgBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
                        )

                        // Draw finished strokes
                        for (stroke in strokes) {
                            if (stroke.points.size < 2) continue
                            val path = Path().apply {
                                moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            val drawColor = if (stroke.isHighlighter) stroke.color.copy(alpha = 0.45f) else stroke.color
                            drawPath(
                                path = path,
                                color = drawColor,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Draw active dragging stroke
                        if (currentPoints.size >= 2) {
                            val path = Path().apply {
                                moveTo(currentPoints[0].x, currentPoints[0].y)
                                for (i in 1 until currentPoints.size) {
                                    lineTo(currentPoints[i].x, currentPoints[i].y)
                                }
                            }
                            val drawColor = if (isHighlighter) selectedColor.copy(alpha = 0.45f) else selectedColor
                            drawPath(
                                path = path,
                                color = drawColor,
                                style = Stroke(
                                    width = strokeSize,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Tool Strip: Mode toggles, Color Swatches, Stroke width
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = PanelElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Tool Mode: Pen vs Highlighter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Pen Mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isHighlighter) Brass else Panel)
                                    .clickable {
                                        isHighlighter = false
                                        strokeSize = 8f
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Pen",
                                    color = if (!isHighlighter) Graphite else Pearl,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Highlighter Mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isHighlighter) Brass else Panel)
                                    .clickable {
                                        isHighlighter = true
                                        strokeSize = 24f
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Highlighter",
                                    color = if (isHighlighter) Graphite else Pearl,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Stroke Width Selector (Fine, Med, Thick)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val widths = if (isHighlighter) listOf(16f to "S", 24f to "M", 36f to "L") else listOf(5f to "S", 10f to "M", 18f to "L")
                            widths.forEach { (w, label) ->
                                val active = strokeSize == w
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (active) Brass else Panel)
                                        .clickable { strokeSize = w },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Graphite else Pearl
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color Palette Swatches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { c ->
                            val isSelected = selectedColor == c
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Pearl else LineBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = c }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a composite Bitmap containing the original screenshot with all drawn doodles rendered.
 */
private fun renderCompositeBitmap(
    sourceBitmap: Bitmap,
    strokes: List<DrawnStroke>
): Bitmap {
    val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    for (stroke in strokes) {
        if (stroke.points.size < 2) continue
        paint.strokeWidth = stroke.strokeWidth
        paint.color = stroke.color.value.toLong().toInt()
        paint.alpha = if (stroke.isHighlighter) 110 else 255

        val path = android.graphics.Path().apply {
            moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                lineTo(stroke.points[i].x, stroke.points[i].y)
            }
        }
        canvas.drawPath(path, paint)
    }

    return result
}
