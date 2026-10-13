package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalLayoutApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFloatingKeyboardLiftRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun keyboardAnimationUpdatesPlacementWithoutRecomposingTheHost() {
        val ime = MutableWindowInsets(WindowInsets(bottom = 0))
        var hostCompositions = 0
        composeRule.setContent {
            val lift = rememberAppFloatingKeyboardLiftState(
                imeInsets = ime,
                navigationBarInsets = WindowInsets(bottom = 0),
                focusedLift = 18.dp,
                restingBottomGap = 0.dp,
                label = "test_lift",
            )
            SideEffect { hostCompositions++ }
            Box(Modifier.offset { IntOffset(0, lift.value.roundToPx()) }.size(48.dp).testTag("dock"))
        }
        composeRule.waitForIdle()
        val initialCompositions = hostCompositions
        repeat(10) { step ->
            composeRule.runOnIdle { ime.insets = WindowInsets(bottom = (step + 1) * 40) }
            composeRule.waitForIdle()
        }
        val top = composeRule.onNodeWithTag("dock").fetchSemanticsNode().boundsInRoot.top
        assertEquals(400f + with(composeRule.density) { 18.dp.toPx() }, top, absoluteTolerance = 1f)
        assertEquals(initialCompositions, hostCompositions, "IME frames must not recompose the dock host")

        composeRule.runOnIdle { ime.insets = WindowInsets(bottom = 0) }
        composeRule.waitForIdle()
        assertEquals(0f, composeRule.onNodeWithTag("dock").fetchSemanticsNode().boundsInRoot.top, absoluteTolerance = 1f)
    }

    @Test
    fun alreadyVisibleKeyboardHasTheCorrectInitialLift() {
        composeRule.setContent {
            val lift = rememberAppFloatingKeyboardLiftState(
                imeInsets = WindowInsets(bottom = 400),
                navigationBarInsets = WindowInsets(bottom = 0),
                focusedLift = 18.dp,
                restingBottomGap = 0.dp,
                label = "test_initial_lift",
            )
            Box(Modifier.offset { IntOffset(0, lift.value.roundToPx()) }.size(48.dp).testTag("dock"))
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.waitForIdle()
        assertEquals(
            400f + with(composeRule.density) { 18.dp.toPx() },
            composeRule.onNodeWithTag("dock").fetchSemanticsNode().boundsInRoot.top,
            absoluteTolerance = 1f,
        )
    }

    @Test
    fun overlappingImeUpdatesFinishAtTheLatestTarget() {
        val ime = MutableWindowInsets(WindowInsets(bottom = 0))
        var hostCompositions = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val lift = rememberAppFloatingKeyboardLiftState(
                imeInsets = ime,
                navigationBarInsets = WindowInsets(bottom = 0),
                focusedLift = 18.dp,
                restingBottomGap = 24.dp,
                label = "test_retargeted_lift",
            )
            SideEffect { hostCompositions++ }
            Box(Modifier.offset { IntOffset(0, lift.value.roundToPx()) }.size(48.dp).testTag("dock"))
        }
        composeRule.waitForIdle()
        val initialCompositions = hostCompositions
        repeat(10) { step ->
            composeRule.runOnIdle { ime.insets = WindowInsets(bottom = (step + 1) * 40) }
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
        }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        assertEquals(
            400f - with(composeRule.density) { 6.dp.toPx() },
            composeRule.onNodeWithTag("dock").fetchSemanticsNode().boundsInRoot.top,
            absoluteTolerance = 1f,
        )
        assertEquals(initialCompositions, hostCompositions)
    }
}
