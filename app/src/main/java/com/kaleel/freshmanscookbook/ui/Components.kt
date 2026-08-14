package com.kaleel.freshmanscookbook.ui

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun RecipeImage(path: String?, modifier: Modifier = Modifier, cornerRadius: Int = 20) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, path) {
        value = path?.let {
            runCatching {
                val uri = android.net.Uri.fromFile(File(it))
                if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                else @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp)).background(WarmPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Herb.copy(alpha = .45f), modifier = Modifier.size(44.dp))
        }
    }
}

fun moveItem(listSize: Int, index: Int, direction: Int): Int? {
    val target = index + direction
    return target.takeIf { it in 0 until listSize }
}

fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }

fun <T, K> List<T>.movedByStableId(id: K, to: Int, key: (T) -> K): List<T> {
    val from = indexOfFirst { key(it) == id }
    if (from < 0 || isEmpty()) return this
    val destination = to.coerceIn(indices)
    return if (from == destination) this else moved(from, destination)
}

/** Determine the final insertion slot without mutating the list mid-gesture. */
fun insertionIndexForPointer(
    orderedIds: List<String>,
    draggedId: String,
    pointerY: Float,
    bounds: Map<String, Pair<Float, Float>>
): Int {
    if (orderedIds.isEmpty()) return -1
    val otherIds = orderedIds.filterNot { it == draggedId }
    val measured = otherIds.mapIndexedNotNull { insertionPosition, id ->
        val (top, bottom) = bounds[id] ?: return@mapIndexedNotNull null
        insertionPosition to (top + bottom) / 2f
    }
    if (measured.isEmpty()) return orderedIds.indexOf(draggedId).coerceAtLeast(0)
    var insertion = measured.first().first
    for ((position, center) in measured) {
        if (pointerY <= center) break
        insertion = position + 1
    }
    return insertion.coerceIn(0, orderedIds.lastIndex)
}
