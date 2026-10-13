package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppPageScaffoldInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchHeightAnimationMovesPaddingWithoutRecomposingTheBody() {
        val visible = mutableStateOf(false)
        var bodyCompositions = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppPageScaffold(
                    title = "Page title",
                    searchBarVisible = visible.value,
                    searchBarContent = { Box(Modifier.fillMaxWidth().height(AppChromeTokens.searchBarHostHeight)) },
                ) { padding ->
                    SideEffect { bodyCompositions++ }
                    Box(Modifier.fillMaxSize().padding(appPageContentPadding(padding))) {
                        Box(Modifier.size(24.dp).testTag("body-marker"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val hiddenTop = composeRule.onNodeWithTag("body-marker").fetchSemanticsNode().boundsInRoot.top
        composeRule.runOnIdle { visible.value = true }
        composeRule.waitForIdle()
        // Finish the visibility transition's setup composition before measuring animation frames.
        composeRule.mainClock.advanceTimeBy(64)
        composeRule.waitForIdle()
        val initialCompositions = bodyCompositions
        repeat(20) {
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
        }
        assertEquals(initialCompositions, bodyCompositions, "The animated inset belongs to layout")
        assertEquals(
            hiddenTop + with(composeRule.density) { AppChromeTokens.searchBarHostHeight.toPx() },
            composeRule.onNodeWithTag("body-marker").fetchSemanticsNode().boundsInRoot.top,
            absoluteTolerance = 1f,
        )
    }

    @Test
    fun topBarNavigationWinsHitTestingOverOverlappingBodyContent() {
        var navigationClicks = 0
        var bodyClicks = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppPageScaffold(
                    title = "Page title",
                    navigationIcon = {
                        Box(
                            Modifier
                                .size(52.dp)
                                .testTag("navigation")
                                .clickable { navigationClicks++ },
                        )
                    },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable { bodyClicks++ },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("navigation").performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(1, navigationClicks)
            assertEquals(0, bodyClicks)
        }
    }

    @Test
    fun searchBarWinsHitTestingAndKeepsOneActiveContentInstance() {
        var searchClicks = 0
        var bodyClicks = 0
        var activeSearchContentInstances = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppPageScaffold(
                    title = "Page title",
                    searchBarVisible = true,
                    searchBarContent = {
                        DisposableEffect(Unit) {
                            activeSearchContentInstances++
                            onDispose { activeSearchContentInstances-- }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(AppChromeTokens.searchBarHostHeight)
                                .testTag("search-content")
                                .clickable { searchClicks++ },
                        )
                    },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable { bodyClicks++ },
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search-content").performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(1, searchClicks)
            assertEquals(0, bodyClicks)
            assertEquals(1, activeSearchContentInstances)
        }
    }

    @Test
    fun searchBarPlaceholderKeepsScaffoldTopPaddingInSyncWithOverlay() {
        val searchVisible = mutableStateOf(false)
        var contentPadding: PaddingValues = PaddingValues()
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppPageScaffold(
                    title = "Page title",
                    searchBarVisible = searchVisible.value,
                    searchBarContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(AppChromeTokens.searchBarHostHeight),
                        )
                    },
                ) { padding ->
                    SideEffect {
                        contentPadding = padding
                    }
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        val hiddenTopPadding: Dp = contentPadding.calculateTopPadding()

        composeRule.runOnIdle { searchVisible.value = true }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val contentTopPadding = contentPadding.calculateTopPadding()
            assertTrue(
                contentTopPadding >= hiddenTopPadding + AppChromeTokens.searchBarHostHeight * 0.95f,
                "Expected visible search placeholder to reserve its animated height, hidden=$hiddenTopPadding visible=$contentTopPadding",
            )
        }
    }
}
