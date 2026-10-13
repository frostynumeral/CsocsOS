@file:Suppress("ktlint:standard:property-naming")

package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.status.AppStatusColors

@Composable
fun rememberAppFloatingKeyboardLift(
    focusedLift: Dp = 18.dp,
    restingBottomGap: Dp = 0.dp,
    label: String = "app_floating_keyboard_lift",
): Dp =
    rememberAppFloatingKeyboardLiftState(
        focusedLift = focusedLift,
        restingBottomGap = restingBottomGap,
        label = label,
    ).value

@Composable
fun rememberAppFloatingKeyboardLiftState(
    focusedLift: Dp = 18.dp,
    restingBottomGap: Dp = 0.dp,
    label: String = "app_floating_keyboard_lift",
): State<Dp> = rememberAppFloatingKeyboardLiftState(
    imeInsets = WindowInsets.ime,
    navigationBarInsets = WindowInsets.navigationBars,
    focusedLift = focusedLift,
    restingBottomGap = restingBottomGap,
    label = label,
)

@Composable
internal fun rememberAppFloatingKeyboardLiftState(
    imeInsets: WindowInsets,
    navigationBarInsets: WindowInsets,
    focusedLift: Dp,
    restingBottomGap: Dp,
    label: String,
): State<Dp> {
    val density = LocalDensity.current
    val targetLift = remember(imeInsets, navigationBarInsets, density, focusedLift, restingBottomGap) {
        {
            with(density) {
                appFloatingKeyboardLiftTarget(
                    imeBottom = imeInsets.getBottom(this).toDp(),
                    navigationBottom = navigationBarInsets.getBottom(this).toDp(),
                    focusedLift = focusedLift,
                    restingBottomGap = restingBottomGap,
                )
            }
        }
    }
    val lift = remember {
        // Match animateDpAsState's initial value even when mounted with the keyboard already open.
        // This one-time read must not subscribe the host to every frame of the IME animation;
        // snapshotFlow below owns that observation for the rest of the lifetime.
        Snapshot.withoutReadObservation {
            Animatable(targetLift(), Dp.VectorConverter, label = label)
        }
    }
    LaunchedEffect(lift, targetLift) {
        snapshotFlow(targetLift).collectLatest { target ->
            if (lift.value != target) {
                lift.animateTo(target, tween(durationMillis = AppFloatingKeyboardLiftMotionMs))
            }
        }
    }
    return lift.asState()
}

fun appFloatingKeyboardLiftTarget(
    imeBottom: Dp,
    navigationBottom: Dp,
    focusedLift: Dp,
    restingBottomGap: Dp = 0.dp,
): Dp {
    if (imeBottom <= navigationBottom) return 0.dp
    return (imeBottom + focusedLift - restingBottomGap).coerceAtLeast(focusedLift)
}

@Composable
fun rememberAppFloatingDockBottomState(
    contentBottomPadding: Dp,
    bottomBarVisible: Boolean,
    label: String,
    bottomGap: Dp = AppFloatingDockBottomGap,
): State<Dp> =
    animateDpAsState(
        targetValue =
            appFloatingDockBottomTarget(
                contentBottomPadding = contentBottomPadding,
                bottomBarVisible = bottomBarVisible,
                bottomGap = bottomGap,
            ),
        label = label,
    )

fun appFloatingDockBottomTarget(
    contentBottomPadding: Dp,
    bottomBarVisible: Boolean,
    bottomGap: Dp = AppFloatingDockBottomGap,
): Dp {
    val bottomBarOffset =
        if (bottomBarVisible) {
            0.dp
        } else {
            AppChromeTokens.floatingBottomBarOuterHeight
        }
    val compactBaselineAdjustment =
        if (bottomBarVisible) {
            0.dp
        } else {
            AppFloatingDockCompactBottomBaselineAdjustment
        }
    return contentBottomPadding - bottomGap - bottomBarOffset - compactBaselineAdjustment
}

fun appFloatingVerticalDockHeight(
    itemSize: Dp,
    itemCount: Int,
): Dp {
    if (itemCount <= 0) return 0.dp
    return itemSize * itemCount
}

@Composable
fun appFloatingRefreshTint(
    status: AppFloatingRefreshStatus,
    enabled: Boolean,
    neutral: Color,
    muted: Color,
    success: Color,
    danger: Color,
    active: Color,
): Color =
    when (status) {
        AppFloatingRefreshStatus.Refreshing -> AppStatusColors.Refreshing
        AppFloatingRefreshStatus.Success -> success
        AppFloatingRefreshStatus.Danger -> danger
        AppFloatingRefreshStatus.Cached -> AppStatusColors.Cached
        AppFloatingRefreshStatus.Idle -> if (enabled) neutral else muted
    }

private const val AppFloatingKeyboardLiftMotionMs = 160
private val AppFloatingDockBottomGap = 24.dp
// Keeps floating compact docks on the same visual baseline as MainPagerBottomBar compact content.
private val AppFloatingDockCompactBottomBaselineAdjustment = 18.dp
