package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.ui.insertionIndexForPointer
import com.kaleel.freshmanscookbook.ui.movedByStableId
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientOrderingTest {
    private data class Item(val id: String)
    private val items = listOf("A", "B", "C", "D").map(::Item)
    private val bounds = mapOf(
        "A" to (0f to 50f),
        "B" to (50f to 100f),
        "C" to (100f to 150f),
        "D" to (150f to 200f)
    )

    @Test
    fun draggingBeneathLastItemMovesOnlyDraggedIdDownward() {
        val target = insertionIndexForPointer(items.map(Item::id), "B", 210f, bounds)
        val result = items.movedByStableId("B", target, Item::id)

        assertEquals(listOf("A", "C", "D", "B"), result.map(Item::id))
    }

    @Test
    fun draggingAboveFirstItemMovesOnlyDraggedIdUpward() {
        val target = insertionIndexForPointer(items.map(Item::id), "D", -10f, bounds)
        val result = items.movedByStableId("D", target, Item::id)

        assertEquals(listOf("D", "A", "B", "C"), result.map(Item::id))
    }

    @Test
    fun stableIdDeterminesSourceEvenWhenIndexChanges() {
        val alreadyChanged = listOf(Item("A"), Item("C"), Item("B"), Item("D"))

        assertEquals(
            listOf("A", "C", "D", "B"),
            alreadyChanged.movedByStableId("B", 3, Item::id).map(Item::id)
        )
    }

    @Test
    fun offscreenUnmeasuredRowsKeepTheirRelativePositions() {
        val longList = listOf("A", "B", "C", "D", "E", "F", "G")
        val visibleBounds = mapOf("E" to (100f to 150f), "F" to (150f to 200f), "G" to (200f to 250f))

        assertEquals(4, insertionIndexForPointer(longList, "E", 120f, visibleBounds))
        assertEquals(5, insertionIndexForPointer(longList, "E", 180f, visibleBounds))
    }
}
