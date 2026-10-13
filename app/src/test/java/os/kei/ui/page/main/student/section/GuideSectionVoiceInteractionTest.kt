package os.kei.ui.page.main.student.section

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.ui.page.main.student.BaGuideVoiceEntry
import os.kei.ui.page.main.student.page.state.BaStudentGuideVoiceUiState
import os.kei.ui.page.main.student.page.state.rememberBaStudentGuideVoicePlaybackState
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w360dp-h800dp-xxhdpi",
)
class GuideSectionVoiceInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playbackProgressRedrawsTheRingWithoutRecomposingThePage() {
        val voice = mutableStateOf(BaStudentGuideVoiceUiState(PLAYBACK_URL, true))
        var pageCompositions = 0
        var drawnProgress = -1f
        var toggledUrl = ""
        composeRule.setContent {
            val playback by rememberBaStudentGuideVoicePlaybackState(voice)
            val progress = remember(voice) {
                {
                    voice.value.voicePlayProgress.also { drawnProgress = it }
                }
            }
            SideEffect { pageCompositions++ }
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                    GuideVoiceEntryCard(
                        entry = BaGuideVoiceEntry(section = "Test section", title = "Playing voice", lines = listOf("Voice line")),
                        languageHeaders = listOf("Alpha"),
                        backdrop = null,
                        playbackUrl = playback.playingVoiceUrl,
                        isPlaying = playback.isVoicePlaying,
                        playProgress = progress,
                        onTogglePlay = { toggledUrl = it },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val initialCompositions = pageCompositions
        repeat(10) { step ->
            composeRule.runOnIdle { voice.value = voice.value.copy(voicePlayProgress = (step + 1) / 10f) }
            composeRule.waitForIdle()
        }
        composeRule.runOnIdle {
            assertEquals(initialCompositions, pageCompositions)
            assertEquals(1f, drawnProgress, "The ring must still draw the latest playback progress")
            voice.value = voice.value.copy(isVoicePlaying = false)
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.onNodeWithContentDescription(context.getString(R.string.guide_action_play)).performClick()
        composeRule.runOnIdle { assertEquals(PLAYBACK_URL, toggledUrl) }
    }

    @Test
    fun languageCardKeepsOnlyCopyAndLanguageActions() {
        var selectedHeader = ""
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                    GuideVoiceLanguageCard(
                        headers = listOf("Alpha", "Beta"),
                        selectedHeader = "Alpha",
                        backdrop = null,
                        onSelected = { selectedHeader = it },
                        modifier = Modifier.testTag(LANGUAGE_CARD_TAG),
                    )
                }
            }
        }

        val languageCardTree =
            hasTestTag(LANGUAGE_CARD_TAG) or hasAnyAncestor(hasTestTag(LANGUAGE_CARD_TAG))
        composeRule
            .onAllNodes(languageCardTree and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(3)
        composeRule
            .onAllNodes(
                languageCardTree and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick) and
                    hasClickAction(),
                useUnmergedTree = true,
            ).assertCountEquals(1)

        composeRule
            .onNodeWithText("Beta")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals("Beta", selectedHeader) }
    }

    @Test
    fun entryCardKeepsOnlyCopyAndPlaybackActions() {
        var toggledUrl = ""
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                    GuideVoiceEntryCard(
                        entry =
                            BaGuideVoiceEntry(
                                section = "Test section",
                                title = "Test title",
                                lineHeaders = listOf("Alpha"),
                                lines = listOf("Test voice line"),
                            ),
                        languageHeaders = listOf("Alpha"),
                        backdrop = null,
                        playbackUrl = PLAYBACK_URL,
                        isPlaying = false,
                        playProgress = { 0f },
                        onTogglePlay = { toggledUrl = it },
                        modifier = Modifier.testTag(ENTRY_CARD_TAG),
                    )
                }
            }
        }

        val entryCardTree =
            hasTestTag(ENTRY_CARD_TAG) or hasAnyAncestor(hasTestTag(ENTRY_CARD_TAG))
        composeRule
            .onAllNodes(entryCardTree and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(3)
        composeRule
            .onAllNodes(
                entryCardTree and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick) and
                    hasClickAction(),
                useUnmergedTree = true,
            ).assertCountEquals(2)

        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.guide_action_play))
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(PLAYBACK_URL, toggledUrl) }
    }
}

private const val LANGUAGE_CARD_TAG = "guide-voice-language-card"
private const val ENTRY_CARD_TAG = "guide-voice-entry-card"
private const val PLAYBACK_URL = "https://example.com/test-voice.mp3"
