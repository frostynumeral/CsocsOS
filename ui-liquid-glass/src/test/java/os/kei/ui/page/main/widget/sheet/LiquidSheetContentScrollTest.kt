package os.kei.ui.page.main.widget.sheet

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidSheetContentScrollTest {
    @Test
    fun nestedScrollPositionsRemainVisibleRegardlessOfAttachmentOrder() {
        val link = LiquidSheetContentScroll()
        val outer = Source(canScrollUp = false, overflows = true)
        val inner = Source(canScrollUp = true, overflows = true)
        link.attach(inner)
        link.attach(outer)
        assertTrue(link.canScrollUp)
        assertTrue(link.overflows)
        inner.canScrollUp = false
        assertFalse(link.canScrollUp)
        outer.canScrollUp = true
        assertTrue(link.canScrollUp)
    }

    @Test
    fun disposingANestedViewportKeepsTheOuterScrollPosition() {
        val link = LiquidSheetContentScroll()
        val outer = Source(canScrollUp = true, overflows = true)
        val inner = Source(canScrollUp = false, overflows = false)
        link.attach(outer)
        link.attach(inner)
        link.detach(inner)
        assertTrue(link.canScrollUp)
        assertTrue(link.overflows)
        link.detach(outer)
        assertFalse(link.canScrollUp)
        assertFalse(link.overflows)
    }

    @Test
    fun repeatedAttachmentDoesNotLeaveAGhostScrollSource() {
        val link = LiquidSheetContentScroll()
        val source = Source(canScrollUp = true, overflows = true)
        link.attach(source)
        link.attach(source)
        link.detach(source)
        assertFalse(link.canScrollUp)
        assertFalse(link.overflows)
    }

    private class Source(
        override var canScrollUp: Boolean,
        override var overflows: Boolean,
    ) : LiquidSheetContentScroll.Source
}
