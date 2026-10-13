package os.kei.ui.page.main.student.catalog.component

import android.app.Application
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeTracingApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class BaGuideBgmMiniPlayerRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()
    private var playerCompositions = 0

    @Before
    fun tracePlayer() {
        Composer.setTracer(object : CompositionTracer {
            override fun isTraceInProgress() = true
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if ("BaGuideBgmMiniPlayer " in info) playerCompositions++
            }
            override fun traceEventEnd() = Unit
        })
    }

    @After
    fun clearTracer() = Composer.setTracer(null)

    @Test
    fun playbackTicksUpdateTheClockAndSliderWithoutRecomposingThePlayer() {
        val runtime = mutableStateOf(BaGuideBgmPlaybackRuntimeState(durationMs = 120_000L))
        var playbackClicks = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                BaGuideBgmMiniPlayer(
                    favorite = FAVORITE, runtimeState = runtime,
                    queueIndex = 0, queueSize = 1, queueMode = BaGuideBgmQueueMode.Continuous,
                    accent = Color(0xFF3B82F6), expanded = true,
                    onExpandedChange = {}, onOpenQueue = {}, onPrevious = {},
                    onTogglePlayback = { playbackClicks++ }, onNext = {}, onSeekChanged = {},
                    onSeekFinished = {}, onVolumeChanged = {}, onSliderInteractionChanged = {},
                    onToggleQueueMode = {}, onOpenGuide = {},
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue(playerCompositions > 0)
        val initialCompositions = playerCompositions
        repeat(9) { step ->
            composeRule.runOnIdle { runtime.value = runtime.value.copy(positionMs = (step + 1) * 200L) }
            composeRule.waitForIdle()
        }
        assertEquals(initialCompositions, playerCompositions)
        composeRule.onNodeWithText("00:01 / 02:00").assertExists()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val slider = composeRule.onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_seekbar))
        assertEquals(0.015f, slider.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo].current, 0.002f)
        composeRule.runOnIdle { runtime.value = runtime.value.copy(isPlaying = true) }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_pause)).performClick()
        assertEquals(1, playbackClicks)
        assertTrue(playerCompositions > initialCompositions, "Playback controls must still follow runtime changes")
    }
}

private val FAVORITE = GuideBgmFavoriteItem(
    audioUrl = "file:///tmp/test-bgm.mp3", title = "Test BGM", studentTitle = "Test student",
    studentImageUrl = "", imageUrl = "", sourceUrl = "", note = "", favoritedAtMs = 0L,
)
