package os.kei.ui.page.main.ba.card

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
class BaAccountPagerRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val compositions = IntArray(2)
    private val effects = BooleanArray(2)
    private lateinit var pager: PagerState
    private lateinit var scope: CoroutineScope

    @Test
    fun scrollingWithinAnActivationRangeDoesNotRecomposeAccountContent() {
        composeRule.setContent {
            pager = rememberPagerState(pageCount = { 2 })
            scope = rememberCoroutineScope()
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                modifier = Modifier.size(300.dp, 100.dp),
            ) { page -> EffectsProbe(page) }
        }
        composeRule.waitForIdle()
        assertTrue(compositions.all { it > 0 })
        assertTrue(effects[0])
        assertFalse(effects[1])
        scrollTo(0, 0.05f)
        assertTrue(effects.all { it })
        val initialCompositions = compositions.toList()
        repeat(8) { step -> scrollTo(0, 0.1f + step * 0.04f) }
        assertEquals(initialCompositions, compositions.toList(), "Only crossing the effects threshold should recompose the content")
        scrollTo(1, 0f)
        assertFalse(effects[0])
        assertTrue(effects[1])
        scrollTo(0, 0f)
        assertTrue(effects[0])
        assertFalse(effects[1])
    }

    @Composable
    private fun EffectsProbe(page: Int) {
        val enabled = rememberBaAccountPageEffectsEnabled(pager, page).value
        SideEffect {
            compositions[page]++
            effects[page] = enabled
        }
        Box(Modifier.size(300.dp, 100.dp))
    }

    private fun scrollTo(page: Int, offset: Float) {
        lateinit var scroll: Job
        composeRule.runOnIdle { scroll = scope.launch { pager.scrollToPage(page, offset) } }
        composeRule.waitUntil { scroll.isCompleted }
        composeRule.waitForIdle()
    }
}
