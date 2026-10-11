package com.fuellog.app.ui.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.fuellog.app.ocr.NormalizedRoi
import com.fuellog.app.ocr.RoiCorner
import com.fuellog.app.ocr.RoiStore
import com.fuellog.app.ocr.RoiTarget
import java.io.File

/**
 * Normalized image coordinate ROI editor. Drag inside the frame to move it;
 * drag any corner handle to resize it.
 */
@Composable
fun RoiEditorDialog(
    imageFile: File,
    roiStore: RoiStore,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val bitmap = remember(imageFile.absolutePath) { BitmapFactory.decodeFile(imageFile.absolutePath) }
    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("ROI設定") },
            text = { Text("画像を開けませんでした。") },
            confirmButton = { Button(onClick = onDismiss) { Text("閉じる") } }
        )
        return
    }

    DisposableEffect(bitmap) {
        onDispose { if (!bitmap.isRecycled) bitmap.recycle() }
    }

    val rois = remember(imageFile.absolutePath) {
        mutableStateListOf<NormalizedRoi>().apply {
            addAll(roiStore.load().filter { it.target.editable })
            if (isEmpty()) addAll(defaultRois())
        }
    }
    var selected by remember { mutableStateOf(RoiTarget.DASHBOARD) }
    val active = rois.firstOrNull { it.target == selected }
    val scroll = rememberScrollState()
    var resizeCorner by remember { mutableStateOf<RoiCorner?>(null) }
    var dragRoi by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(1f) }
    val imageHorizontalScroll = rememberScrollState()
    val imageVerticalScroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("読み取り範囲（ROI）設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("対象を選び、枠内をドラッグして移動、四隅の丸をドラッグしてサイズを変更します。")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoiTarget.entries.filter { it.editable }.forEach { target ->
                        FilterChip(
                            selected = selected == target,
                            onClick = {
                                selected = target
                                if (rois.none { it.target == target }) {
                                    rois.add(defaultRoi(target))
                                }
                            },
                            label = { Text(target.label) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = {
                        rois.clear()
                        rois.addAll(defaultRois())
                        if (rois.none { it.target == selected }) rois.add(defaultRoi(selected))
                    }) { Text("初期配置に戻す") }
                }
                if (active != null) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("画像の拡大", modifier = Modifier.padding(end = 8.dp))
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1f..3f,
                            steps = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Text(String.format(java.util.Locale.US, "%.1fx", zoom), modifier = Modifier.padding(start = 6.dp))
                    }
                    Text("拡大時は画像をスクロールして細かく調整できます。", color = Color.Gray)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        val scaledWidth = maxWidth * zoom
                        val scaledHeight = scaledWidth * (bitmap.height.toFloat() / bitmap.width.toFloat())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .horizontalScroll(imageHorizontalScroll)
                                .verticalScroll(imageVerticalScroll)
                        ) {
                            Box(modifier = Modifier.width(scaledWidth).height(scaledHeight)) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "ROI編集対象画像",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                                )
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(selected, bitmap.width, bitmap.height) {
                                    detectDragGestures(
                                        onDragStart = { position ->
                                            val current = rois.firstOrNull { it.target == selected }
                                            val radius = 36.dp.toPx()
                                            resizeCorner = current?.let { roi ->
                                                listOf(
                                                    RoiCorner.TOP_LEFT to Offset(roi.left * size.width, roi.top * size.height),
                                                    RoiCorner.TOP_RIGHT to Offset(roi.right * size.width, roi.top * size.height),
                                                    RoiCorner.BOTTOM_LEFT to Offset(roi.left * size.width, roi.bottom * size.height),
                                                    RoiCorner.BOTTOM_RIGHT to Offset(roi.right * size.width, roi.bottom * size.height)
                                                ).firstOrNull { (_, point) ->
                                                    kotlin.math.abs(position.x - point.x) <= radius &&
                                                        kotlin.math.abs(position.y - point.y) <= radius
                                                }?.first
                                            }
                                            dragRoi = current?.let { roi ->
                                                position.x in (roi.left * size.width)..(roi.right * size.width) &&
                                                    position.y in (roi.top * size.height)..(roi.bottom * size.height)
                                            } == true || resizeCorner != null
                                        },
                                        onDragEnd = { resizeCorner = null; dragRoi = false },
                                        onDragCancel = { resizeCorner = null; dragRoi = false }
                                    ) { change, dragAmount ->
                                        if (!dragRoi) {
                                            // ROI枠の外をドラッグした場合は、拡大画像をパンして隠れた部分を表示する。
                                            imageHorizontalScroll.dispatchRawDelta(-dragAmount.x)
                                            imageVerticalScroll.dispatchRawDelta(-dragAmount.y)
                                            change.consume()
                                            return@detectDragGestures
                                        }
                                        change.consume()
                                        val current = rois.firstOrNull { it.target == selected } ?: return@detectDragGestures
                                        val dx = dragAmount.x / size.width
                                        val dy = dragAmount.y / size.height
                                        val updated = resizeCorner?.let { current.resizeFrom(it, dx, dy) }
                                            ?: current.move(dx, dy)
                                        val index = rois.indexOfFirst { it.target == selected }
                                        if (index >= 0) rois[index] = updated
                                    }
                                }
                                ) {
                                    rois.forEach { roi ->
                                        val isActive = roi.target == selected
                                        val color = if (isActive) Color(0xFFFFC107) else Color(0xFF4FC3F7)
                                        drawRect(
                                            color = color.copy(alpha = if (isActive) 0.9f else 0.35f),
                                            topLeft = Offset(roi.left * size.width, roi.top * size.height),
                                            size = androidx.compose.ui.geometry.Size(
                                                (roi.right - roi.left) * size.width,
                                                (roi.bottom - roi.top) * size.height
                                            ),
                                            style = Stroke(width = if (isActive) 3.dp.toPx() else 1.dp.toPx())
                                        )
                                        if (isActive) {
                                            listOf(
                                                Offset(roi.left * size.width, roi.top * size.height),
                                                Offset(roi.right * size.width, roi.top * size.height),
                                                Offset(roi.left * size.width, roi.bottom * size.height),
                                                Offset(roi.right * size.width, roi.bottom * size.height)
                                            ).forEach { point ->
                                                drawCircle(color = color, radius = 8.dp.toPx(), center = point)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                roiStore.save(rois.toList())
                onSaved()
            }) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                onDismiss()
            }) { Text("キャンセル") }
        }
    )
}

private fun defaultRois() = listOf(
    NormalizedRoi(RoiTarget.DASHBOARD, .05f, .25f, .95f, .75f),
    NormalizedRoi(RoiTarget.FUEL_AMOUNT, .40f, .30f, .90f, .42f),
    NormalizedRoi(RoiTarget.UNIT_PRICE, .40f, .42f, .90f, .54f),
    NormalizedRoi(RoiTarget.TOTAL, .40f, .54f, .90f, .66f),
    NormalizedRoi(RoiTarget.DATE, .35f, .05f, .95f, .18f)
)

private fun defaultRoi(target: RoiTarget) = when (target) {
    RoiTarget.ODOMETER_INTEGER -> NormalizedRoi(target, .15f, .35f, .75f, .52f)
    RoiTarget.ODOMETER_DECIMAL -> NormalizedRoi(target, .70f, .35f, .88f, .52f)
    RoiTarget.TRIP_INTEGER -> NormalizedRoi(target, .15f, .48f, .75f, .65f)
    RoiTarget.TRIP_DECIMAL -> NormalizedRoi(target, .70f, .48f, .88f, .65f)
    RoiTarget.DASHBOARD -> NormalizedRoi(target, .05f, .25f, .95f, .75f)
    RoiTarget.FUEL_AMOUNT -> NormalizedRoi(target, .40f, .30f, .90f, .42f)
    RoiTarget.UNIT_PRICE -> NormalizedRoi(target, .40f, .42f, .90f, .54f)
    RoiTarget.TOTAL -> NormalizedRoi(target, .40f, .54f, .90f, .66f)
    RoiTarget.DATE -> NormalizedRoi(target, .35f, .05f, .95f, .18f)
    RoiTarget.WHOLE -> NormalizedRoi(target, 0f, 0f, 1f, 1f)
}
