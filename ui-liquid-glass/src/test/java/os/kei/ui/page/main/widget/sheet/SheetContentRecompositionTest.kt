package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SheetContentRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resizingAnOverflowingViewportOnlyRequiresLayout() {
        val viewportHeight = mutableIntStateOf(400)
        val reports = mutableListOf<Boolean>()
        composeRule.setContent {
            Box(Modifier.fillMaxWidth().layout { measurable, constraints ->
                val height = viewportHeight.intValue.coerceAtMost(constraints.maxHeight)
                val child = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(child.width, height) { child.place(0, 0) }
            }) {
                CompositionLocalProvider(LocalLiquidSheetContentOverflowReporter provides { reports.add(it) }) {
                    SheetContentColumn(verticalSpacing = 0.dp) {
                        GrayRows(count = 24, height = 48.dp)
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val recomposers = Recomposer.runningRecomposers.value.toList()
        assertTrue(recomposers.isNotEmpty())
        val changesBefore = recomposers.sumOf { it.changeCount }
        val reportsBefore = reports.toList()

        repeat(8) { step ->
            composeRule.runOnIdle { viewportHeight.intValue = 380 - step * 20 }
            composeRule.waitForIdle()
        }

        assertEquals(
            changesBefore,
            recomposers.sumOf { it.changeCount },
            "Resizing a scroll viewport must not recompose its content container",
        )
        assertEquals(reportsBefore, reports, "Overflow must only be reported when its boolean value changes")
    }

    @Test
    fun draggingTheSheetBothWaysKeepsContentAndChromeOutOfRecomposition() {
        composeRule.setContent {
            LiquidSheetTestTheme {
                LiquidGlassBottomSheet(show = true, title = "Sheet") {
                    SheetContentColumn(verticalSpacing = 0.dp) { GrayRows(count = 24, height = 48.dp) }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        val initialBounds = composeRule.nodeBounds(LiquidSheetPanelTestTag)
        val recomposers = Recomposer.runningRecomposers.value.toList()
        assertTrue(recomposers.isNotEmpty())
        val initialChanges = recomposers.sumOf { it.changeCount }

        composeRule.onNodeWithTag(LiquidSheetDragRegionTestTag).performTouchInput {
            down(Offset(width / 2f, 10.dp.toPx()))
            repeat(8) { moveBy(Offset(0f, 12.dp.toPx())) }
            up()
        }
        composeRule.waitForIdle()
        val shorterBounds = composeRule.nodeBounds(LiquidSheetPanelTestTag)
        assertTrue(shorterBounds.top > initialBounds.top + 40.dp)
        assertEquals(initialBounds.bottom.value, shorterBounds.bottom.value, absoluteTolerance = 1f)

        composeRule.onNodeWithTag(LiquidSheetDragRegionTestTag).performTouchInput {
            down(Offset(width / 2f, 10.dp.toPx()))
            repeat(8) { moveBy(Offset(0f, -12.dp.toPx())) }
            up()
        }
        composeRule.waitForIdle()
        val restoredBounds = composeRule.nodeBounds(LiquidSheetPanelTestTag)
        assertTrue(restoredBounds.top < shorterBounds.top - 40.dp)
        assertEquals(initialBounds.bottom.value, restoredBounds.bottom.value, absoluteTolerance = 1f)
        assertEquals(initialChanges, recomposers.sumOf { it.changeCount })
    }

    @Test
    fun viewportChangesStillReportBothOverflowTransitions() {
        val viewportHeight = mutableIntStateOf(400)
        val reports = mutableListOf<Boolean>()
        composeRule.setContent {
            Box(Modifier.fillMaxWidth().layout { measurable, constraints ->
                val height = viewportHeight.intValue.coerceAtMost(constraints.maxHeight)
                val child = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(child.width, height) { child.place(0, 0) }
            }) {
                CompositionLocalProvider(LocalLiquidSheetContentOverflowReporter provides { reports.add(it) }) {
                    SheetContentColumn(verticalSpacing = 0.dp) {
                        GrayRows(count = 1, height = 48.dp)
                    }
                }
            }
        }
        composeRule.waitForIdle()
        reports.clear()
        composeRule.runOnIdle { viewportHeight.intValue = 60 }
        composeRule.waitForIdle()
        composeRule.runOnIdle { viewportHeight.intValue = 400 }
        composeRule.waitForIdle()

        assertEquals(listOf(true, false), reports)
    }

    @Test
    fun lazyScrollEdgesDoNotReportFalseOverflowOrDetachTheLiveSource() {
        val contentScroll = LiquidSheetContentScroll()
        val reports = mutableListOf<Boolean>()
        composeRule.setContent {
            Box(Modifier.fillMaxWidth().layout { measurable, constraints ->
                val child = measurable.measure(constraints.copy(minHeight = 300, maxHeight = 300))
                layout(child.width, child.height) { child.place(0, 0) }
            }) {
                CompositionLocalProvider(
                    LocalLiquidSheetContentOverflowReporter provides { reports.add(it) },
                    LocalLiquidSheetContentScroll provides contentScroll,
                ) {
                    SheetContentLazyColumn(Modifier.testTag("lazy-content"), verticalSpacing = 0.dp) {
                        items(count = 20, key = { it }) { GrayRows(count = 1, height = 48.dp) }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val reportsBefore = reports.toList()
        composeRule.onNodeWithTag("lazy-content").performScrollToIndex(19)
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(contentScroll.canScrollUp)
            assertTrue(contentScroll.overflows)
        }
        composeRule.onNodeWithTag("lazy-content").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertFalse(contentScroll.canScrollUp)
            assertTrue(contentScroll.overflows)
            assertEquals(reportsBefore, reports)
        }
    }

    @Test
    fun replacingReportersPublishesCurrentStateAndDisposesTheLatestReporter() {
        val show = mutableStateOf(true)
        val reporterVersion = mutableIntStateOf(0)
        val overflow = listOf(mutableListOf<Boolean>(), mutableListOf<Boolean>())
        val managed = listOf(mutableListOf<Boolean>(), mutableListOf<Boolean>())
        composeRule.setContent {
            val version = reporterVersion.intValue
            if (show.value) {
                Box(Modifier.fillMaxWidth().layout { measurable, constraints ->
                    val child = measurable.measure(constraints.copy(minHeight = 300, maxHeight = 300))
                    layout(child.width, child.height) { child.place(0, 0) }
                }) {
                    CompositionLocalProvider(
                        LocalLiquidSheetContentOverflowReporter provides { overflow[version].add(it) },
                        LocalLiquidSheetManagedScrollableContentReporter provides { managed[version].add(it) },
                    ) {
                        SheetContentColumn(verticalSpacing = 0.dp) { GrayRows(count = 12, height = 48.dp) }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val originalOverflow = overflow[0].toList()
        val originalManaged = managed[0].toList()
        composeRule.runOnIdle { reporterVersion.intValue = 1 }
        composeRule.waitForIdle()
        assertEquals(listOf(true), overflow[1])
        assertEquals(listOf(true), managed[1])
        composeRule.runOnIdle { show.value = false }
        composeRule.waitForIdle()
        assertEquals(listOf(true, false), overflow[1])
        assertEquals(listOf(true, false), managed[1])
        assertEquals(originalOverflow, overflow[0])
        assertEquals(originalManaged, managed[0])
    }
}
