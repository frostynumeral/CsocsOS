package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
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
@OptIn(InternalComposeTracingApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w1000dp-h900dp-xxhdpi")
class AppPageInsetRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val scopes = mutableListOf<String>()

    @Before
    fun traceLayouts() {
        Composer.setTracer(object : CompositionTracer {
            override fun isTraceInProgress() = true
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if (LAYOUT_NAMES.any { "$it " in info }) scopes.add(info)
            }
            override fun traceEventEnd() = Unit
        })
    }

    @After
    fun clearTracer() = Composer.setTracer(null)

    @Test
    fun changingInsetsMovesSingleAndDualColumnContentWithoutRecomposingLayouts() {
        val topInset = mutableStateOf(20.dp)
        val padding = object : PaddingValues {
            override fun calculateTopPadding() = topInset.value
            override fun calculateBottomPadding() = 12.dp
            override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
            override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
        }
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        AppPageLazyColumn(padding, rememberLazyListState(), Modifier.height(180.dp)) {
                            item { Box(Modifier.size(24.dp).testTag("single-marker")) }
                        }
                        AppPageTwoColumnLists(
                            padding, rememberLazyListState(), rememberLazyListState(), Modifier.height(180.dp),
                            header = { Box(Modifier.size(24.dp).testTag("header-marker")) },
                            primary = { item { Box(Modifier.size(24.dp)) } },
                            secondary = { item { Box(Modifier.size(24.dp)) } },
                        )
                        AppPageTwoColumnLists(
                            padding, rememberLazyListState(), rememberLazyListState(), Modifier.height(180.dp),
                            primary = { item { Box(Modifier.size(24.dp).testTag("dual-marker")) } },
                            secondary = { item { Box(Modifier.size(24.dp)) } },
                        )
                        AppPageTwoColumnPanes(
                            padding, Modifier.height(180.dp),
                            primary = { Box(Modifier.size(24.dp).testTag("pane-marker")) },
                            secondary = { Box(Modifier.size(24.dp)) },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        LAYOUT_NAMES.forEach { name -> assertTrue(scopes.any { "$name " in it }, "Tracer must capture $name") }
        val initialTops = MARKER_TAGS.map { composeRule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        scopes.clear()
        repeat(8) { step ->
            composeRule.runOnIdle { topInset.value = (24 + step * 4).dp }
            composeRule.waitForIdle()
        }
        assertEquals(emptyList(), scopes, "Insets belong to list measurement and padding layout")
        val movement = with(composeRule.density) { 32.dp.toPx() }
        MARKER_TAGS.forEachIndexed { index, tag ->
            assertEquals(initialTops[index] + movement, composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top, 1f)
        }
    }
}

private val LAYOUT_NAMES = listOf("AppPageLazyColumn", "AppPageTwoColumnLists", "AppPageTwoColumnPanes")
private val MARKER_TAGS = listOf("single-marker", "header-marker", "dual-marker", "pane-marker")
