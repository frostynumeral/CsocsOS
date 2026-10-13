package os.kei.ui.page.main.student.section.gallery

import android.app.Application
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.student.GuideGalleryAudioPlaybackRepository
import os.kei.ui.page.main.student.GuideMediaProgressState
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeTracingApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class GuideGalleryAudioRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val scopes = mutableListOf<String>()
    private lateinit var audio: GuideGalleryAudioPlayerState

    @Before
    fun traceAudioScopes() {
        Composer.setTracer(object : CompositionTracer {
            override fun isTraceInProgress() = true
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if ("GuideGalleryCardContent " in info || "BindGuideGalleryAudioPlayerEffects " in info) scopes.add(info)
            }
            override fun traceEventEnd() = Unit
        })
    }

    @After
    fun clearTracer() = Composer.setTracer(null)

    @Test
    fun progressTicksAndTimeLabelsDoNotRecomposeTheGalleryCard() {
        composeRule.setContent {
            val context = LocalContext.current
            audio = remember {
                GuideGalleryAudioPlayerState(AUDIO_URL, context,
                    GuideGalleryAudioPlaybackRepository(context, "audio-recomposition-card", AUDIO_URL))
                    .apply { durationMs = 120_000L }
            }
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                GuideGalleryCardContent(
                    backdrop = null, normalizedMediaType = "audio", displayTitle = "Test audio",
                    mediaTypeLabel = "Audio", showMediaTypeLabel = true, audioTargetUrl = AUDIO_URL,
                    displayMediaUrl = AUDIO_URL, displayImageUrl = "", canSaveMedia = false,
                    saveTargetUrl = "", isImageType = false, imageLoading = false, onImageLoadingChanged = {},
                    imageProgressState = remember { GuideMediaProgressState(1f) }, notePlainText = "Audio note",
                    noteLinks = emptyList(), canOpenMedia = false, itemMediaUrl = AUDIO_URL,
                    onOpenMedia = {}, onSaveMedia = { _, _ -> }, showBgmFavoriteAction = false,
                    isBgmFavorite = false, bgmFavoriteContentDescription = "Favorite", onToggleBgmFavorite = {},
                    showAudioLoopAction = false, onToggleAudioLoop = {}, audioState = audio,
                    gestureState = remember { GuideGalleryGestureState() },
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue(scopes.any { "GuideGalleryCardContent " in it })
        scopes.clear()
        repeat(9) { step ->
            composeRule.runOnIdle {
                audio.positionMs = (step + 1) * 200L
                audio.playProgress = audio.positionMs / 120_000f
            }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("00:01").assertExists()
        composeRule.onNodeWithText("02:00").assertExists()
        assertEquals(0, scopes.count { "GuideGalleryCardContent " in it })
    }

    @Test
    fun seekPreviewDeltasDoNotRestartThePollingEffect() {
        composeRule.setContent {
            val context = LocalContext.current
            audio = remember {
                GuideGalleryAudioPlayerState(AUDIO_URL, context,
                    GuideGalleryAudioPlaybackRepository(context, "audio-recomposition-effects", AUDIO_URL))
            }
            BindGuideGalleryAudioPlayerEffects(audio)
        }
        composeRule.waitForIdle()
        assertTrue(scopes.any { "BindGuideGalleryAudioPlayerEffects " in it })
        composeRule.runOnIdle { audio.seekProgress = 0.1f }
        composeRule.waitForIdle()
        scopes.clear()
        repeat(8) { step ->
            composeRule.runOnIdle { audio.seekProgress = (step + 2) / 10f }
            composeRule.waitForIdle()
        }
        assertEquals(0, scopes.count { "BindGuideGalleryAudioPlayerEffects " in it })
        composeRule.runOnIdle { audio.seekProgress = null }
        composeRule.waitForIdle()
        assertEquals(1, scopes.count { "BindGuideGalleryAudioPlayerEffects " in it })
    }
}

private const val AUDIO_URL = "file:///tmp/test-audio.mp3"
