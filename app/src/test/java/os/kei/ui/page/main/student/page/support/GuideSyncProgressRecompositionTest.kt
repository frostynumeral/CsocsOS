package os.kei.ui.page.main.student.page.support

import android.app.Application
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class GuideSyncProgressRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingAnimationOnlyUpdatesTheProgressIndicator() {
        var pageCompositions = 0
        var displayedProgress = -1f
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val progress = rememberGuideSyncProgressState(loading = true, animationsEnabled = true)
            SideEffect { pageCompositions++ }
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LiquidCircularProgressBar(progress = { progress.value.also { displayedProgress = it } })
            }
        }
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.waitForIdle()
        val initialCompositions = pageCompositions
        repeat(20) {
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
        }
        assertTrue(displayedProgress > 0.12f)
        assertEquals(initialCompositions, pageCompositions, "Loading progress must not recompose the route")
        composeRule.mainClock.advanceTimeBy(3_000)
        composeRule.waitForIdle()
        assertEquals(0.9f, displayedProgress, absoluteTolerance = 0.001f)
    }
}
