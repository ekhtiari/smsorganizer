package com.ekhtiari.smsorganizer.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

object SoftMotion {
    const val DurationMs = 400
    const val FastMs = 280
    const val StaggerMs = 40

    fun <T> tweenSoft(duration: Int = DurationMs) = tween<T>(
        durationMillis = duration,
        easing = FastOutSlowInEasing,
    )

    fun enterForward(): EnterTransition =
        fadeIn(animationSpec = tweenSoft()) +
            slideInHorizontally(
                animationSpec = tweenSoft(),
                initialOffsetX = { it / 12 },
            )

    fun exitForward(): ExitTransition =
        fadeOut(animationSpec = tweenSoft(FastMs)) +
            slideOutHorizontally(
                animationSpec = tweenSoft(FastMs),
                targetOffsetX = { -it / 16 },
            )

    fun enterPop(): EnterTransition =
        fadeIn(animationSpec = tweenSoft()) +
            slideInHorizontally(
                animationSpec = tweenSoft(),
                initialOffsetX = { -it / 16 },
            )

    fun exitPop(): ExitTransition =
        fadeOut(animationSpec = tweenSoft(FastMs)) +
            slideOutHorizontally(
                animationSpec = tweenSoft(FastMs),
                targetOffsetX = { it / 12 },
            )

    fun fadeSlideUp(): EnterTransition =
        fadeIn(animationSpec = tweenSoft()) +
            slideInVertically(
                animationSpec = tweenSoft(),
                initialOffsetY = { it / 10 },
            )

    fun fadeSlideDown(): ExitTransition =
        fadeOut(animationSpec = tweenSoft(FastMs)) +
            slideOutVertically(
                animationSpec = tweenSoft(FastMs),
                targetOffsetY = { -it / 14 },
            )

    fun <S> tabContentTransform(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        val forward = targetState.hashCode() >= initialState.hashCode()
        fadeIn(animationSpec = tweenSoft()) +
            slideInHorizontally(
                animationSpec = tweenSoft(),
                initialOffsetX = { if (forward) it / 8 else -it / 8 },
            ) togetherWith
            fadeOut(animationSpec = tweenSoft(FastMs)) +
                slideOutHorizontally(
                    animationSpec = tweenSoft(FastMs),
                    targetOffsetX = { if (forward) -it / 10 else it / 10 },
                )
    }
}
