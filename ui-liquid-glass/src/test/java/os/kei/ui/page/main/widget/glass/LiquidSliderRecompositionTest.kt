package os.kei.ui.page.main.widget.glass

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeTracingApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LiquidSliderRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val composedScopes = mutableListOf<String>()

    @Before
    fun traceSliderScopes() {
        Composer.setTracer(object : CompositionTracer {
            override fun isTraceInProgress() = true
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if ("LiquidTrackSlider" in info || "LiquidSliderKeyPointDot" in info) composedScopes.add(info)
            }
            override fun traceEventEnd() = Unit
        })
    }

    @After
    fun clearTracer() {
        Composer.setTracer(null)
    }

    @Test
    fun lightLtrKeyPointCrossingsOnlyRecomposeTheDots() = verifyProgressUpdates(false)

    @Test
    fun darkRtlKeyPointCrossingsOnlyRecomposeTheDots() = verifyProgressUpdates(true)

    private fun verifyProgressUpdates(darkRtl: Boolean) {
        val progress = mutableFloatStateOf(0f)
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(if (darkRtl) ColorSchemeMode.Dark else ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalTransitionAnimationsEnabled provides false,
                    LocalLayoutDirection provides if (darkRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    LiquidKeyPointSlider(
                        value = { progress.floatValue },
                        onValueChange = { progress.floatValue = it },
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = null,
                        keyPoints = listOf(
                            LiquidSliderKeyPoint(0f),
                            LiquidSliderKeyPoint(0.25f),
                            LiquidSliderKeyPoint(0.5f, Color.Red),
                            LiquidSliderKeyPoint(0.75f),
                            LiquidSliderKeyPoint(1f),
                        ),
                        modifier = Modifier.height(48.dp).testTag("slider"),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue(composedScopes.any { "LiquidTrackSlider" in it }, "The runtime tracer must capture the slider")
        composedScopes.clear()
        val values = listOf(0.1f, 0.3f, 0.6f, 0.9f, 1f, 0f)
        values.forEachIndexed { index, value ->
            composeRule.runOnIdle { progress.floatValue = value }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("slider").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(value, 0f..1f)),
            )
            // Optional before/after pixel evidence for this focused performance investigation.
            System.getenv("KEIOS_COMPOSE_CAPTURE_DIR")?.let { directory ->
                val file = File(directory, "slider-${if (darkRtl) "dark-rtl" else "light-ltr"}-$index.png")
                requireNotNull(file.parentFile).mkdirs()
                file.outputStream().use { output ->
                    composeRule.onNodeWithTag("slider").captureToImage().asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            }
        }
        assertEquals(
            0,
            composedScopes.count { "LiquidTrackSlider" in it },
            "Crossing a key point must not recompose the slider's material and gesture tree",
        )
        assertTrue(composedScopes.any { "LiquidSliderKeyPointDot" in it }, "The dots must still update their colors")
    }
}
