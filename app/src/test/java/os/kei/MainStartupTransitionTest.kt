package os.kei

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import android.window.SplashScreen
import android.window.SplashScreenView
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class MainStartupTransitionTest {
    @Test fun `prepared destination draws even before pager frame activation`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { activity ->
            val transition = MainStartupTransition(activity.get())
            transition.onContentReady(ready = true, transitionAnimationsEnabled = true, waitForMainPage = true)
            // Android returns true when any pre-draw listener cancels this draw. Blocking it
            // here would also prevent the pager from receiving the frame it needs to activate.
            assertFalse(activity.get().window.decorView.viewTreeObserver.dispatchOnPreDraw())
            assertTrue(transition.hasPresented)
            transition.dispose()
        }
    }

    @Test fun `pending local state holds the first draw but releases after the deadline`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { activity ->
            val transition = MainStartupTransition(activity.get())
            val observer = activity.get().window.decorView.viewTreeObserver
            assertTrue(observer.dispatchOnPreDraw())
            assertFalse(transition.hasPresented)
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
            assertFalse(observer.dispatchOnPreDraw())
            assertTrue(transition.hasPresented)
            transition.dispose()
        }
    }

    @Test fun `completed launch releases the listener before notification or shortcut reentry`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val splashScreen = RecordingSplashScreen()
            val transition = MainStartupTransition(activity, splashScreen)
            transition.onContentReady(ready = true, transitionAnimationsEnabled = true, waitForMainPage = true)
            transition.onMainPageReady()
            activity.window.decorView.viewTreeObserver.dispatchOnPreDraw()
            val listener = splashScreen.listener!!
            val initial = newSplash(activity)
            listener.onSplashScreenExit(initial)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

            assertEquals(View.GONE, initial.visibility)
            assertNull(splashScreen.listener)
            repeat(3) {
                val late = newSplash(activity)
                listener.onSplashScreenExit(late)
                assertEquals(View.GONE, late.visibility)
            }
            transition.dispose()
        }
    }

    @Test fun `late repeated splash transfers are removed after the first reveal`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val splashScreen = RecordingSplashScreen()
            val transition = MainStartupTransition(activity, splashScreen)
            transition.onContentReady(ready = true, transitionAnimationsEnabled = false, waitForMainPage = false)
            val listener = splashScreen.listener!!
            val first = newSplash(activity)
            listener.onSplashScreenExit(first)
            assertEquals(View.GONE, first.visibility)

            repeat(3) {
                val next = newSplash(activity)
                listener.onSplashScreenExit(next)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
                assertEquals(View.GONE, next.visibility)
            }
            transition.dispose()
        }
    }

    @Test fun `draw deadline removes the splash and releases listener even without local state`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val splashScreen = RecordingSplashScreen()
            val transition = MainStartupTransition(activity, splashScreen)
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
            activity.window.decorView.viewTreeObserver.dispatchOnPreDraw()
            val splash = newSplash(activity)
            splashScreen.listener!!.onSplashScreenExit(splash)
            assertEquals(View.GONE, splash.visibility)
            assertNull(splashScreen.listener)
            transition.dispose()
        }
    }

    @Test fun `overlapping transfer cannot replace the splash owned by the pending reveal`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val splashScreen = RecordingSplashScreen()
            val transition = MainStartupTransition(activity, splashScreen)
            transition.onContentReady(ready = true, transitionAnimationsEnabled = true, waitForMainPage = true)
            val listener = splashScreen.listener!!
            val initial = newSplash(activity)
            listener.onSplashScreenExit(initial)
            val overlapping = newSplash(activity)
            listener.onSplashScreenExit(overlapping)
            assertEquals(View.GONE, overlapping.visibility)
            assertEquals(View.VISIBLE, initial.visibility)

            // Even without a pager-ready callback, the bounded reveal must finish.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(View.GONE, initial.visibility)
            assertNull(splashScreen.listener)
            transition.dispose()
        }
    }

    @Test fun `destroyed activity releases listener and removes an already queued transfer`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val splashScreen = RecordingSplashScreen()
            val transition = MainStartupTransition(activity, splashScreen)
            val listener = splashScreen.listener!!
            transition.dispose()
            assertNull(splashScreen.listener)
            val splash = newSplash(activity)
            listener.onSplashScreenExit(splash)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(View.GONE, splash.visibility)
        }
    }

    @Test fun `dispose during reveal removes the owned splash and pending animation callbacks`() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val splashScreen = RecordingSplashScreen()
            val transition = MainStartupTransition(activity, splashScreen)
            transition.onContentReady(ready = true, transitionAnimationsEnabled = true, waitForMainPage = false)
            val listener = splashScreen.listener!!
            val splash = newSplash(activity)
            listener.onSplashScreenExit(splash)
            transition.dispose()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(View.GONE, splash.visibility)
            assertNull(splashScreen.listener)
            val late = newSplash(activity)
            listener.onSplashScreenExit(late)
            assertEquals(View.GONE, late.visibility)
        }
    }

    private class RecordingSplashScreen : SplashScreen {
        var listener: SplashScreen.OnExitAnimationListener? = null
            private set

        override fun setOnExitAnimationListener(listener: SplashScreen.OnExitAnimationListener) {
            this.listener = listener
        }

        override fun clearOnExitAnimationListener() {
            listener = null
        }

        override fun setSplashScreenTheme(themeId: Int) = Unit
    }

    private fun newSplash(activity: ComponentActivity): SplashScreenView =
        ReflectionHelpers.callConstructor(
            SplashScreenView::class.java,
            ClassParameter.from(Context::class.java, activity),
        )
}
