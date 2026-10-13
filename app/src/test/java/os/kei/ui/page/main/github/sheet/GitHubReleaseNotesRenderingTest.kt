package os.kei.ui.page.main.github.sheet

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import os.kei.R
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.page.GitHubDecisionAssistDetailRequest
import os.kei.ui.page.main.github.page.GitHubDecisionAssistDetailType
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.markdown.parseAppMarkdownBlocks
import os.kei.ui.page.main.widget.sheet.LiquidSheetDragRegionTestTag
import os.kei.ui.page.main.widget.sheet.LiquidSheetPanelTestTag
import os.kei.ui.page.main.widget.sheet.SceneBackdropHost
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w360dp-h804dp-xxhdpi")
class GitHubReleaseNotesRenderingTest {
    @get:Rule
    val composeRule = createComposeRule()
    private var openedUrl = ""

    @Test
    fun longKernelSuNotesKeepTheGlassSurfaceWithinTheViewport() {
        showNotes(kernelSuNotes())
        composeRule.onNodeWithText("What's Changed").assertIsDisplayed()
        assertBodyWithinViewport()
        captureFixture("long-phone")
        composeRule.onNodeWithTag(GitHubReleaseNotesListTestTag).performScrollToNode(hasText(FULL_CHANGELOG, substring = true))
        composeRule.onNodeWithText(FULL_CHANGELOG, substring = true).performScrollTo()
        composeRule.onNodeWithText(FULL_CHANGELOG, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(FULL_CHANGELOG, substring = true).performTouchInput { click(center) }
        assertEquals(FULL_CHANGELOG, openedUrl)

        val context = ApplicationProvider.getApplicationContext<Application>()
        composeRule.onNodeWithText(context.getString(R.string.common_copy)).performClick()
        composeRule.runOnIdle {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertContains(clipboard.primaryClip!!.getItemAt(0).text.toString(), kernelSuNotes().trim())
        }
    }

    @Test
    fun largeFontNotesKeepTheGlassSurfaceWithinTheViewport() {
        showNotes(kernelSuNotes(), fontScale = 1.5f)
        composeRule.onNodeWithText("What's Changed").assertIsDisplayed()
        assertBodyWithinViewport()
        captureFixture("long-large-font")
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun tabletNotesKeepTheGlassSurfaceWithinTheViewport() {
        showNotes(kernelSuNotes(), dark = true, fontScale = 1.5f)
        assertBodyWithinViewport()
        captureFixture("long-tablet-dark")
    }

    @Test
    fun collapsedNotesCanStillScrollPastTheSummaryToTheDocument() {
        showNotes(kernelSuNotes(), fontScale = 1.5f)
        composeRule.onNodeWithTag(LiquidSheetDragRegionTestTag).performSemanticsAction(SemanticsActions.Collapse) { it() }
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.waitForIdle()
        val collapsedHeight = composeRule.onNodeWithTag(LiquidSheetPanelTestTag).fetchSemanticsNode().boundsInRoot.height
        // Swipe the visible summary: at a small detent the document may initially be below it.
        composeRule.onNodeWithTag(GitHubReleaseNotesContentTestTag).performTouchInput { swipeUp(durationMillis = 700) }
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.waitForIdle()
        assertTrue(composeRule.onNodeWithTag(LiquidSheetPanelTestTag).fetchSemanticsNode().boundsInRoot.height > collapsedHeight)
        composeRule.onNodeWithText("What's Changed").performScrollTo().assertIsDisplayed()
        assertBodyWithinViewport()
    }

    @Test
    fun longNotesKeepBothWaySheetResizingAndContentDragOwnership() {
        showNotes(kernelSuNotes())
        val panel = composeRule.onNodeWithTag(LiquidSheetPanelTestTag)
        val initialBounds = panel.fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag(LiquidSheetDragRegionTestTag).performTouchInput {
            down(Offset(width / 2f, 10.dp.toPx()))
            repeat(8) { moveBy(Offset(0f, 12.dp.toPx())) }
            up()
        }
        composeRule.waitForIdle()
        val shorterBounds = panel.fetchSemanticsNode().boundsInRoot
        assertTrue(shorterBounds.height < initialBounds.height - 100f)
        assertEquals(initialBounds.bottom, shorterBounds.bottom, absoluteTolerance = 2f)

        composeRule.onNodeWithTag(LiquidSheetDragRegionTestTag).performTouchInput {
            down(Offset(width / 2f, 10.dp.toPx()))
            repeat(8) { moveBy(Offset(0f, -40.dp.toPx())) }
            up()
        }
        composeRule.waitForIdle()
        val expandedBounds = panel.fetchSemanticsNode().boundsInRoot
        assertTrue(expandedBounds.height > initialBounds.height + 100f)
        assertEquals(initialBounds.bottom, expandedBounds.bottom, absoluteTolerance = 2f)

        // A downward drag in the middle of the long document belongs to its list, not to resizing.
        val list = composeRule.onNodeWithTag(GitHubReleaseNotesListTestTag)
        list.performScrollToNode(hasText(FULL_CHANGELOG, substring = true))
        composeRule.onNodeWithText(FULL_CHANGELOG, substring = true).performScrollTo().assertIsDisplayed()
        val beforeContentDrag = panel.fetchSemanticsNode().boundsInRoot
        list.performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(0f, 6.dp.toPx())) }
            up()
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        assertEquals(beforeContentDrag.height, panel.fetchSemanticsNode().boundsInRoot.height, absoluteTolerance = 2f)
        assertBodyWithinViewport()
    }

    @Test
    fun richMarkdownKeepsTableLinksTasksQuotesAndCode() {
        showNotes(RICH_NOTES)
        val list = composeRule.onNodeWithTag(GitHubReleaseNotesListTestTag)
        list.performScrollToNode(hasText("Open table link"))
        composeRule.onNodeWithText("Open table link").performScrollTo().performTouchInput { click(center) }
        assertEquals("https://example.com/table", openedUrl)
        list.performScrollToNode(hasText("Verified APK"))
        composeRule.onNodeWithText("Verified APK").performScrollTo().assertIsDisplayed()
        list.performScrollToNode(hasText("A quoted note"))
        composeRule.onNodeWithText("A quoted note").performScrollTo().assertIsDisplayed()
        list.performScrollToNode(hasText("val version = 3"))
        composeRule.onNodeWithText("val version = 3").performScrollTo().assertIsDisplayed()
        assertBodyWithinViewport()
    }

    @Test
    fun emptyReleaseNotesKeepTheirVisibleEmptyMessage() {
        showNotes("")
        val context = ApplicationProvider.getApplicationContext<Application>()
        composeRule.onNodeWithText(context.getString(R.string.github_release_notes_detail_empty)).assertIsDisplayed()
        assertBodyWithinViewport()
    }

    @Test
    fun releaseMetadataFallbackLinesRemainVisible() {
        showNotes("", fallbackLines = listOf("Fallback version", "Release metadata"))
        composeRule.onNodeWithText("Fallback version").assertIsDisplayed()
        composeRule.onNodeWithText("Release metadata", substring = true).assertIsDisplayed()
        assertBodyWithinViewport()
    }

    @Test
    fun shortLightNotesKeepTheirAdaptiveHeightAndAppearance() {
        showNotes(SHORT_NOTES, fallback = true)
        assertBodyWithinViewport()
        captureFixture("short-light")
    }

    @Test
    fun shortDarkNotesKeepTheirAdaptiveHeightAndAppearance() {
        showNotes(SHORT_NOTES, dark = true, fallback = true)
        assertBodyWithinViewport()
        captureFixture("short-dark")
    }

    private fun showNotes(markdown: String, dark: Boolean = false, fontScale: Float = 1f, fallback: Boolean = false, fallbackLines: List<String> = emptyList()) {
        val item = GitHubTrackedApp(
            repoUrl = "https://github.com/tiann/KernelSU", owner = "tiann", repo = "KernelSU",
            packageName = "me.weishu.kernelsu", appLabel = "KernelSU",
        )
        val bundle = GitHubReleaseAssetBundle(
            releaseName = "v3.3.1", tagName = "v3.3.1", htmlUrl = "https://github.com/tiann/KernelSU/releases/tag/v3.3.1",
            releaseNotesBody = markdown, assets = emptyList(),
        )
        val detail = GitHubReleaseNotesDetailUiState(
            rawMarkdown = markdown, markdownBlocks = parseAppMarkdownBlocks(markdown, preserveLineBreaks = true),
            lines = fallbackLines,
        )
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalTextCopyExpandedOverride provides true,
                LocalDensity provides Density(density.density, fontScale),
                LocalLiquidControlsEnabled provides !fallback,
            ) {
                MiuixTheme(controller = ThemeController(if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light)) {
                    SceneBackdropHost {
                        GitHubDecisionAssistDetailSheet(
                            request = GitHubDecisionAssistDetailRequest(GitHubDecisionAssistDetailType.ReleaseNotes, item),
                            backdrop = emptyBackdrop(), versionState = VersionCheckUi(), assetBundle = bundle,
                            releaseNotesDetailState = detail, assetLoading = false, assetError = "",
                            onDismissRequest = {}, onRefreshHealth = {}, onRefreshReleaseNotes = { _, _ -> },
                            onSelectReleaseNotesTarget = { _, _ -> }, onOpenExternalUrl = { openedUrl = it },
                        )
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(3_000)
        composeRule.waitForIdle()
    }

    private fun assertBodyWithinViewport() {
        val viewportHeight = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { it.boundsInRoot.height }
        val materialHeight = composeRule.onNodeWithTag(GitHubReleaseNotesBodyTestTag).fetchSemanticsNode().layoutInfo.height
        assertTrue(materialHeight > 0)
        assertTrue(materialHeight <= viewportHeight, "Long notes must not create a document-sized material surface: height=$materialHeight, viewport=$viewportHeight")
    }

    private fun captureFixture(name: String) {
        System.getenv("KEIOS_ISSUE30_CAPTURE_DIR")?.let { directory ->
            val file = File(directory, "$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { output ->
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            val body = composeRule.onNodeWithTag(GitHubReleaseNotesBodyTestTag).fetchSemanticsNode().layoutInfo
            File(directory, "$name-size.txt").writeText("body=${body.width}x${body.height}\n")
        }
    }

    private fun kernelSuNotes() = requireNotNull(javaClass.getResource("/github-release-notes/kernelsu-v3.3.1.md")).readText()
}

private const val SHORT_NOTES = "## Short notes\n\nA small release note.\n\n[Open changelog](https://example.com/changelog)"
private const val FULL_CHANGELOG = "https://github.com/tiann/KernelSU/compare/v3.3.0...v3.3.1"
private const val RICH_NOTES = "## Rich notes\n\n| Change | Link |\n| --- | --- |\n| Release | [Open table link](https://example.com/table) |\n\n- [x] Verified APK\n\n> A quoted note\n\n```kotlin\nval version = 3\n```"
