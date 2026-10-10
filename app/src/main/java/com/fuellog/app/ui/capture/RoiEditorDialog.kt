package com.fuellog.app.ui.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.fuellog.app.ocr.RoiStore
import com.fuellog.app.ocr.RoiTarget
import java.io.File

/**
 * Normalized image coordinate ROI editor. Drag inside the frame to move it;
 * drag the bottom-right handle to resize it.
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

    val rois = remember(imageFile.absolutePath) {
        mutableStateListOf<NormalizedRoi>().apply {
            addAll(roiStore.load())
            if (isEmpty()) {
                addAll(
                    listOf(
                        NormalizedRoi(RoiTarget.ODOMETER_INTEGER, .15f, .35f, .75f, .52f),
                        NormalizedRoi(RoiTarget.ODOMETER_DECIMAL, .70f, .35f, .88f, .52f),
                        NormalizedRoi(RoiTarget.TRIP_INTEGER, .15f, .48f, .75f, .65f),
                        NormalizedRoi(RoiTarget.TRIP_DECIMAL, .70f, .48f, .88f, .65f),
                        NormalizedRoi(RoiTarget.FUEL_AMOUNT, .40f, .30f, .90f, .42f),
                        NormalizedRoi(RoiTarget.UNIT_PRICE, .40f, .42f, .90f, .54f),
                        NormalizedRoi(RoiTarget.TOTAL, .40f, .54f, .90f, .66f),
                        NormalizedRoi(RoiTarget.DATE, .35f, .05f, .95f, .18f)
                    )
                )
            }
        }
    }
    var selected by remember { mutableStateOf(RoiTarget.ODOMETER_INTEGER) }
    val active = rois.firstOrNull { it.target == selected }
    val scroll = rememberScrollState()
    var resizing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("読み取り範囲（ROI）設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("対象を選び、枠内をドラッグして移動、右下の丸をドラッグしてサイズを変更します。")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoiTarget.entries.forEach { target ->
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
                if (active != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "ROI編集対象画像",
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matchParentSize()
                                .pointerInput(selected, active, bitmap.width, bitmap.height) {
                                    detectDragGestures(
                                        onDragStart = { position ->
                                            val current = rois.firstOrNull { it.target == selected }
                                            resizing = current != null &&
                                                position.x >= current.right * size.width - 36.dp.toPx() &&
                                                position.y >= current.bottom * size.height - 36.dp.toPx()
                                        },
                                        onDragEnd = { resizing = false },
                                        onDragCancel = { resizing = false }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val current = rois.firstOrNull { it.target == selected } ?: return@detectDragGestures
                                        val dx = dragAmount.x / size.width
                                        val dy = dragAmount.y / size.height
                                        val updated = if (resizing) current.resize(dx, dy) else current.move(dx, dy)
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
                                    drawCircle(
                                        color = color,
                                        radius = 9.dp.toPx(),
                                        center = Offset(roi.right * size.width, roi.bottom * size.height)
                                    )
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
                bitmap.recycle()
                onSaved()
            }) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                bitmap.recycle()
                onDismiss()
            }) { Text("キャンセル") }
        }
    )
}

private fun defaultRoi(target: RoiTarget) = when (target) {
    RoiTarget.ODOMETER_INTEGER -> NormalizedRoi(target, .15f, .35f, .75f, .52f)
    RoiTarget.ODOMETER_DECIMAL -> NormalizedRoi(target, .70f, .35f, .88f, .52f)
    RoiTarget.TRIP_INTEGER -> NormalizedRoi(target, .15f, .48f, .75f, .65f)
    RoiTarget.TRIP_DECIMAL -> NormalizedRoi(target, .70f, .48f, .88f, .65f)
    RoiTarget.FUEL_AMOUNT -> NormalizedRoi(target, .40f, .30f, .90f, .42f)
    RoiTarget.UNIT_PRICE -> NormalizedRoi(target, .40f, .42f, .90f, .54f)
    RoiTarget.TOTAL -> NormalizedRoi(target, .40f, .54f, .90f, .66f)
    RoiTarget.DATE -> NormalizedRoi(target, .35f, .05f, .95f, .18f)
    RoiTarget.WHOLE -> NormalizedRoi(target, 0f, 0f, 1f, 1f)
}
