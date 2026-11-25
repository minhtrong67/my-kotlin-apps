package com.gnort67.wallpaperswitcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CropJob(val uri: Uri, val dark: Boolean, val slot: Int)

private fun screenSize(ctx: Context): Pair<Int, Int> {
    val wm = ctx.getSystemService(WindowManager::class.java)
    val (w, h) = if (Build.VERSION.SDK_INT >= 30) {
        val b = wm.maximumWindowMetrics.bounds; b.width() to b.height()
    } else {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        dm.widthPixels to dm.heightPixels
    }
    return min(w, h) to max(w, h)
}

/** Pan/zoom state; the frame (crop window) is centered, the image moves under it. */
private class CropState(val iw: Int, val ih: Int, val aspect: Float, val pad: Float) {
    var k by mutableFloatStateOf(1f)
    var ox by mutableFloatStateOf(0f)
    var oy by mutableFloatStateOf(0f)
    var bw = 0f
    var bh = 0f

    fun frameH() = min(bh - 2 * pad, (bw - 2 * pad) / aspect)
    fun frameW() = frameH() * aspect
    fun base() = max(frameW() / iw, frameH() / ih)

    fun clamp() {
        val s = base() * k
        val mx = max(0f, (iw * s - frameW()) / 2f)
        val my = max(0f, (ih * s - frameH()) / 2f)
        ox = ox.coerceIn(-mx, mx); oy = oy.coerceIn(-my, my)
    }

    fun transform(c: Offset, pan: Offset, zoom: Float) {
        val nk = (k * zoom).coerceIn(1f, 6f)
        val r = nk / k
        val cx = c.x - bw / 2f
        val cy = c.y - bh / 2f
        ox = (ox - cx) * r + cx + pan.x
        oy = (oy - cy) * r + cy + pan.y
        k = nk
        clamp()
    }

    fun srcRect(): RectF {
        val s = base() * k
        val w = frameW() / s
        val h = frameH() / s
        val l = (iw / 2f - ox / s - w / 2f).coerceIn(0f, max(0f, iw - w))
        val t = (ih / 2f - oy / s - h / 2f).coerceIn(0f, max(0f, ih - h))
        return RectF(l, t, l + w, t + h)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(job: CropJob, onFinish: (Boolean) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val (sw, sh) = remember { screenSize(ctx) }
    val bmp by produceState<Bitmap?>(null, job) {
        value = withContext(Dispatchers.IO) { ImageTools.loadWorking(ctx, job.uri) }
    }
    val padPx = with(LocalDensity.current) { 24.dp.toPx() }
    val b = bmp
    val st = remember(b) { b?.let { CropState(it.width, it.height, sw.toFloat() / sh, padPx) } }
    var saving by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.crop_title)) },
                navigationIcon = {
                    IconButton(onCancel) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
                },
                actions = {
                    Button(
                        onClick = {
                            val s = st ?: return@Button
                            val src = b ?: return@Button
                            val r = s.srcRect()
                            saving = true
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    ImageTools.cropAndSave(src, r, sw, sh, WallpaperApplier.file(ctx, job.dark, job.slot))
                                }
                                onFinish(ok)
                            }
                        },
                        enabled = st != null && !saving
                    ) { Text(stringResource(R.string.save)) }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (b == null || st == null) CircularProgressIndicator()
                else {
                    val img = remember(b) { b.asImageBitmap() }
                    Canvas(
                        Modifier.fillMaxSize()
                            .onSizeChanged { st.bw = it.width.toFloat(); st.bh = it.height.toFloat(); st.clamp() }
                            .pointerInput(st) { detectTransformGestures { c, pan, zoom, _ -> st.transform(c, pan, zoom) } }
                    ) {
                        if (st.bw <= 0f) return@Canvas
                        val fw = st.frameW(); val fh = st.frameH()
                        val s = st.base() * st.k
                        val dw = st.iw * s; val dh = st.ih * s
                        val cx = size.width / 2f + st.ox
                        val cy = size.height / 2f + st.oy
                        drawImage(
                            img, srcSize = IntSize(st.iw, st.ih),
                            dstOffset = IntOffset((cx - dw / 2f).roundToInt(), (cy - dh / 2f).roundToInt()),
                            dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                            filterQuality = FilterQuality.Medium
                        )
                        val fl = (size.width - fw) / 2f; val ft = (size.height - fh) / 2f
                        val corner = CornerRadius(20.dp.toPx())
                        val path = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            addRoundRect(RoundRect(Rect(fl, ft, fl + fw, ft + fh), corner))
                        }
                        drawPath(path, Color.Black.copy(alpha = 0.6f))
                        drawRoundRect(Color.White, Offset(fl, ft), Size(fw, fh), corner, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Text(
                stringResource(R.string.crop_hint),
                Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
