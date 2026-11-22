package com.ekhtiari.smsorganizer.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Material shared-axis and fade-through motion.
 * Horizontal travel flips in RTL so forward motion still comes from the end edge.
 */
object SoftMotion {
    const val DurationMs = 320
    const val FastMs = 200
    const val StaggerMs = 28

    private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> tweenSoft(duration: Int = DurationMs) = tween<T>(
        durationMillis = duration,
        easing = Emphasized,
    )

    fun sharedAxisZEnter(): EnterTransition =
        fadeIn(tween(240, delayMillis = 70, easing = LinearEasing)) +
            scaleIn(animationSpec = tweenSoft(), initialScale = 0.92f)

    fun sharedAxisZExit(): ExitTransition =
        fadeOut(tween(120, easing = LinearEasing)) +
            scaleOut(animationSpec = tweenSoft(FastMs), targetScale = 1.04f)

    fun sharedAxisZPopEnter(): EnterTransition =
        fadeIn(tween(240, delayMillis = 70, easing = LinearEasing)) +
            scaleIn(animationSpec = tweenSoft(), initialScale = 1.04f)

    fun sharedAxisZPopExit(): ExitTransition =
        fadeOut(tween(140, easing = LinearEasing)) +
            scaleOut(animationSpec = tweenSoft(FastMs), targetScale = 0.92f)

    /** Vertical shared axis. [forward] travels upward (compose sheet comes in from below). */
    fun sharedAxisYEnter(forward: Boolean): EnterTransition =
        fadeIn(tween(220, delayMillis = 40, easing = LinearEasing)) +
            slideInVertically(animationSpec = tweenSoft()) { full ->
                if (forward) full / 6 else -full / 14
            }

    fun sharedAxisYExit(forward: Boolean): ExitTransition =
        fadeOut(tween(150, easing = LinearEasing)) +
            slideOutVertically(animationSpec = tweenSoft(FastMs)) { full ->
                if (forward) -full / 14 else full / 6
            }

    fun fadeThroughEnter(): EnterTransition =
        fadeIn(tween(210, delayMillis = 90, easing = LinearEasing)) +
            scaleIn(
                animationSpec = tween(210, delayMillis = 90, easing = Emphasized),
                initialScale = 0.94f,
            )

    fun fadeThroughExit(): ExitTransition =
        fadeOut(tween(90, easing = LinearEasing))

    fun enterForward(): EnterTransition = sharedAxisZEnter()

    fun exitForward(): ExitTransition = sharedAxisZExit()

    fun enterPop(): EnterTransition = sharedAxisZPopEnter()

    fun exitPop(): ExitTransition = sharedAxisZPopExit()

    fun fadeSlideUp(): EnterTransition =
        fadeIn(animationSpec = tweenSoft()) +
            slideInVertically(animationSpec = tweenSoft()) { it / 8 }

    fun fadeSlideDown(): ExitTransition =
        fadeOut(animationSpec = tweenSoft(FastMs)) +
            slideOutVertically(animationSpec = tweenSoft(FastMs)) { -it / 10 }

    /**
     * Shared-axis X for sibling tabs. [indexOf] decides direction; [rtl] mirrors it.
     */
    fun <S> tabContentTransform(
        indexOf: (S) -> Int,
        rtl: Boolean,
    ): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        val forward = indexOf(targetState) >= indexOf(initialState)
        val direction = if (forward != rtl) 1 else -1
        (
            fadeIn(tween(220, delayMillis = 50, easing = LinearEasing)) +
                slideInHorizontally(animationSpec = tweenSoft()) { full -> direction * full / 8 }
            ) togetherWith (
            fadeOut(tween(120, easing = LinearEasing)) +
                slideOutHorizontally(animationSpec = tweenSoft(FastMs)) { full -> -direction * full / 10 }
            )
    }
}
