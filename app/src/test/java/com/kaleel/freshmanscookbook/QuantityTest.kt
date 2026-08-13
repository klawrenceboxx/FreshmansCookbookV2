package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.normalizeFoodSearchName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityTest {
    @Test fun parsesDecimalsAndFractions() {
        assertEquals(1.5, parseQuantity("1.5")!!, 0.0001)
        assertEquals(0.5, parseQuantity("1/2")!!, 0.0001)
        assertEquals(2.0 / 3.0, parseQuantity("2/3")!!, 0.0001)
        assertNull(parseQuantity(""))
    }

    @Test fun formatsCommonFractionsNaturally() {
        assertEquals("1/4", formatQuantity(.25))
        assertEquals("1/2", formatQuantity(.5))
        assertEquals("2", formatQuantity(2.0))
    }

    @Test fun normalizesFoodNamesForSearch() {
        assertEquals("creme fraiche and herbs", normalizeFoodSearchName("Crème fraîche & herbs"))
    }
}
