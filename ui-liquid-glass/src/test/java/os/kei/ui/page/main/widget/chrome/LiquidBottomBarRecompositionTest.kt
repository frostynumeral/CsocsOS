package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeTracingApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LiquidBottomBarRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()
    private var itemCompositions = 0

    @Before
    fun traceItems() {
        Composer.setTracer(object : CompositionTracer {
            override fun isTraceInProgress() = true
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if ("LiquidGlassBottomBarItem " in info) itemCompositions++
            }
            override fun traceEventEnd() = Unit
        })
    }

    @After
    fun clearTracer() = Composer.setTracer(null)

    @Test
    fun continuousPagerPositionDoesNotRecomposeTabItems() {
        val position = mutableFloatStateOf(0f)
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    LiquidGlassBottomBar(
                        modifier = Modifier.size(280.dp, 64.dp),
                        selectedIndex = 0,
                        selectedPositionProvider = { position.floatValue },
                        onSelected = {},
                        backdrop = emptyBackdrop(),
                        tabsCount = 2,
                        isLiquidEffectEnabled = false,
                    ) {
                        repeat(2) { index ->
                            LiquidGlassBottomBarItem(selected = index == 0, tabIndex = index, onClick = {}) {
                                Text("Tab $index")
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue(itemCompositions > 0, "The runtime tracer must capture tab items")
        val initialCompositions = itemCompositions
        repeat(10) { step ->
            composeRule.runOnIdle { position.floatValue = (step + 1) / 10f }
            composeRule.waitForIdle()
        }
        assertEquals(initialCompositions, itemCompositions, "Selection progress belongs to the layer animation")
    }

    @Test
    fun pressingATabAnimatesItsLayerWithoutRecomposingTheItem() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LiquidGlassBottomBar(
                    modifier = Modifier.size(280.dp, 64.dp),
                    selectedIndex = 0,
                    onSelected = {},
                    backdrop = emptyBackdrop(),
                    tabsCount = 2,
                ) {
                    repeat(2) { index ->
                        LiquidGlassBottomBarItem(
                            selected = index == 0, tabIndex = index, onClick = {},
                            modifier = Modifier.testTag("tab-$index"),
                        ) { Text("Tab $index") }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue(itemCompositions > 0)
        val itemText = composeRule.onNode(hasText("Tab 0") and hasAnyAncestor(hasClickAction()), useUnmergedTree = true)
        val restingWidth = itemText.fetchSemanticsNode().boundsInRoot.width
        composeRule.onNodeWithTag("tab-0").performTouchInput { down(center) }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.waitForIdle()
        val initialCompositions = itemCompositions
        repeat(20) {
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
        }
        assertEquals(initialCompositions, itemCompositions, "The press animation belongs to the item layer")
        assertTrue(
            itemText.fetchSemanticsNode().boundsInRoot.width > restingWidth * 1.02f,
            "The selected item must still expand while pressed",
        )
        composeRule.onNodeWithTag("tab-0").performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.waitForIdle()
        assertEquals(restingWidth, itemText.fetchSemanticsNode().boundsInRoot.width, 0.5f)
    }
}
