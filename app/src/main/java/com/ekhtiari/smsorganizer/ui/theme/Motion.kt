package com.ekhtiari.smsorganizer.ui.theme

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.BoundsTransform
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = staticCompositionLocalOf { false }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    return androidx.compose.runtime.remember(resolver) {
        runCatching {
            Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Snap scheme used when the system animator duration scale is 0. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal object ReducedMotionScheme : MotionScheme {
    private val snap = SnapSpec<Any>(delay = 0)

    @Suppress("UNCHECKED_CAST")
    private fun <T> spec(): FiniteAnimationSpec<T> = snap as FiniteAnimationSpec<T>

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spec()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spec()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spec()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = spec()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = spec()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = spec()
}

/**
 * Spring specs aligned with [MotionScheme.expressive]. Navigation transitions
 * cannot read the composition local, so they take [reduce] from the caller.
 */
object SoftMotion {
    private const val SpatialDamping = 0.8f
    private const val SpatialStiffness = 380f
    private const val FastSpatialDamping = 0.6f
    private const val FastSpatialStiffness = 800f
    private const val EffectsDamping = 1f
    private const val EffectsStiffness = 1600f
    private const val FastEffectsStiffness = 3800f

    fun <T> spatial(reduce: Boolean = false, fast: Boolean = false): FiniteAnimationSpec<T> =
        if (reduce) {
            snap()
        } else {
            spring(
                dampingRatio = if (fast) FastSpatialDamping else SpatialDamping,
                stiffness = if (fast) FastSpatialStiffness else SpatialStiffness,
            )
        }

    fun <T> effects(reduce: Boolean = false, fast: Boolean = false): FiniteAnimationSpec<T> =
        if (reduce) {
            snap()
        } else {
            spring(
                dampingRatio = EffectsDamping,
                stiffness = if (fast) FastEffectsStiffness else EffectsStiffness,
            )
        }

    private fun <T> snap(): FiniteAnimationSpec<T> = SnapSpec(delay = 0)

    fun sharedAxisZEnter(reduce: Boolean = false): EnterTransition =
        if (reduce) {
            EnterTransition.None
        } else {
            fadeIn(effects()) + scaleIn(animationSpec = spatial(), initialScale = 0.92f)
        }

    fun sharedAxisZExit(reduce: Boolean = false): ExitTransition =
        if (reduce) {
            ExitTransition.None
        } else {
            fadeOut(effects(fast = true)) + scaleOut(animationSpec = spatial(fast = true), targetScale = 1.04f)
        }

    fun sharedAxisZPopEnter(reduce: Boolean = false): EnterTransition =
        if (reduce) {
            EnterTransition.None
        } else {
            fadeIn(effects()) + scaleIn(animationSpec = spatial(), initialScale = 1.04f)
        }

    fun sharedAxisZPopExit(reduce: Boolean = false): ExitTransition =
        if (reduce) {
            ExitTransition.None
        } else {
            fadeOut(effects(fast = true)) + scaleOut(animationSpec = spatial(fast = true), targetScale = 0.92f)
        }

    fun sharedAxisYEnter(forward: Boolean, reduce: Boolean = false): EnterTransition =
        if (reduce) {
            EnterTransition.None
        } else {
            fadeIn(effects()) + slideInVertically(animationSpec = spatial()) { full ->
                if (forward) full / 5 else -full / 12
            }
        }

    fun sharedAxisYExit(forward: Boolean, reduce: Boolean = false): ExitTransition =
        if (reduce) {
            ExitTransition.None
        } else {
            fadeOut(effects(fast = true)) + slideOutVertically(animationSpec = spatial(fast = true)) { full ->
                if (forward) -full / 12 else full / 5
            }
        }

    fun fadeThroughEnter(reduce: Boolean = false): EnterTransition =
        if (reduce) {
            EnterTransition.None
        } else {
            fadeIn(effects()) + scaleIn(animationSpec = spatial(), initialScale = 0.94f)
        }

    fun fadeThroughExit(reduce: Boolean = false): ExitTransition =
        if (reduce) ExitTransition.None else fadeOut(effects(fast = true))

    fun fadeSlideUp(reduce: Boolean = false): EnterTransition =
        if (reduce) {
            EnterTransition.None
        } else {
            fadeIn(effects()) + slideInVertically(animationSpec = spatial()) { it / 8 }
        }

    fun fadeSlideDown(reduce: Boolean = false): ExitTransition =
        if (reduce) {
            ExitTransition.None
        } else {
            fadeOut(effects(fast = true)) + slideOutVertically(animationSpec = spatial(fast = true)) { -it / 10 }
        }

    /**
     * Shared-axis X for sibling tabs. [indexOf] decides direction; [rtl] mirrors it.
     */
    fun <S> tabContentTransform(
        indexOf: (S) -> Int,
        rtl: Boolean,
        reduce: Boolean = false,
    ): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        if (reduce) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            val forward = indexOf(targetState) >= indexOf(initialState)
            val direction = if (forward != rtl) 1 else -1
            (
                fadeIn(effects()) +
                    slideInHorizontally(animationSpec = spatial()) { full -> direction * full / 6 }
                ) togetherWith (
                fadeOut(effects(fast = true)) +
                    slideOutHorizontally(animationSpec = spatial(fast = true)) { full -> -direction * full / 8 }
                )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedWith(
    key: Any,
    visibilityScope: AnimatedVisibilityScope?,
    bounds: Boolean = false,
): Modifier {
    val shared = LocalSharedTransitionScope.current
    if (shared == null || visibilityScope == null || LocalReducedMotion.current) return this
    val transform = BoundsTransform { _: Rect, _: Rect ->
        spring<Rect>(dampingRatio = 0.8f, stiffness = 380f)
    }
    return with(shared) {
        if (bounds) {
            sharedBounds(
                rememberSharedContentState(key),
                animatedVisibilityScope = visibilityScope,
                boundsTransform = transform,
            )
        } else {
            sharedElement(
                rememberSharedContentState(key),
                animatedVisibilityScope = visibilityScope,
                boundsTransform = transform,
            )
        }
    }
}
