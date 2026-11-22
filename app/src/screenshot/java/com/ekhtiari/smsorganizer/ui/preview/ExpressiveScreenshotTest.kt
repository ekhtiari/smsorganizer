package com.ekhtiari.smsorganizer.ui.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * Renders the gallery previews to PNGs. Not part of CI: the source set is only
 * compiled when RENDER_SCREENSHOTS=1.
 *
 * captureToImage() waits for a draw callback that never runs, because Robolectric
 * executes the test on the main thread. Drawing the window synchronously avoids that.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h960dp-xhdpi")
class ExpressiveScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var forcedNavigationBarDp: Int? = null
    private var forcedStatusBarDp: Int? = null

    @Test
    fun englishLightAndDark() {
        var screen by mutableStateOf<@Composable () -> Unit>({ })
        compose.setContent { screen() }
        capture("inbox-light", { screen = { InboxLightPreview() } })
        capture("inbox-dark", { screen = { InboxDarkPreview() } })
        capture("conversation-light", { screen = { ConversationLightPreview() } })
        capture("conversation-dark", { screen = { ConversationDarkPreview() } })
        capture("compose-light", { screen = { ComposeLightPreview() } })
        capture("compose-dark", { screen = { ComposeDarkPreview() } })
        capture("settings-light", { screen = { SettingsLightPreview() } })
        capture("settings-dark", { screen = { SettingsDarkPreview() } })
        capture("update-light", { screen = { UpdateLightPreview() } })
        capture("update-dark", { screen = { UpdateDarkPreview() } })
    }

    @Test
    fun inboxClearsThreeButtonAndGestureNavigation() {
        var screen by mutableStateOf<@Composable () -> Unit>({ })
        compose.setContent { screen() }

        forcedNavigationBarDp = 48
        forcedStatusBarDp = 24
        val light = capture("inbox-light-3button-nav", { screen = { InboxLightPreview() } })
        assertClearsNavigationBar(light, navigationBarDp = 48, surface = 0xFFF7F9FF.toInt())
        saveBitmap("inbox-light-3button-nav", overlayNavigationBar(light))

        val dark = capture("inbox-dark-3button-nav", { screen = { InboxDarkPreview() } })
        assertClearsNavigationBar(dark, navigationBarDp = 48, surface = 0xFF0E1520.toInt())
        saveBitmap("inbox-dark-3button-nav", overlayNavigationBar(dark))

        val conversation = capture(
            "conversation-light-3button-nav",
            { screen = { ConversationLightPreview() } },
        )
        assertClearsNavigationBar(conversation, navigationBarDp = 48, surface = 0xFFF0F3FC.toInt())
        saveBitmap("conversation-light-3button-nav", overlayNavigationBar(conversation))

        forcedNavigationBarDp = 24
        forcedStatusBarDp = 24
        val gesture = capture("inbox-light-gesture-nav", { screen = { InboxLightPreview() } })
        val gestureGap = bottomClearancePx(gesture, 0xFFF7F9FF.toInt())
        val gestureInset = dpPx(24)
        val threeButtonInset = dpPx(48)
        check(gestureGap >= gestureInset) {
            "Gesture nav inset ${gestureInset}px still covers the toolbar (gap=${gestureGap}px)"
        }
        check(gestureGap < threeButtonInset) {
            "Gesture nav should use the smaller inset, but the gap was ${gestureGap}px"
        }
    }

    private fun capture(name: String, show: () -> Unit, drawNavBar: Boolean = false): Bitmap {
        show()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        val navDp = forcedNavigationBarDp
        val statusDp = forcedStatusBarDp
        if (navDp != null && statusDp != null) {
            pushWindowInsets(navigationBarDp = navDp, statusBarDp = statusDp)
            compose.mainClock.advanceTimeBy(300)
            compose.waitForIdle()
            shadowOf(Looper.getMainLooper()).idle()
        }
        val dialog = ShadowDialog.getLatestDialog()
        val window = if (dialog?.isShowing == true) {
            dialog.window ?: compose.activity.window
        } else {
            compose.activity.window
        }
        val bitmap = draw(window)
        val presented = if (drawNavBar) overlayNavigationBar(bitmap) else bitmap
        saveBitmap(name, presented)
        check(coloredPixels(bitmap) > 500) { "$name rendered almost blank" }
        if (dialog?.isShowing == true) dialog.dismiss()
        return bitmap
    }

    private fun saveBitmap(name: String, bitmap: Bitmap) {
        val dir = File("/opt/cursor/artifacts")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    /**
     * Robolectric has no navigation bar, and [WindowInsetsCompat] drops insets the device
     * cannot show. Compose keeps the real values in an internal holder; write them there
     * after composition so navigationBarsPadding() matches a 3-button or gesture device.
     */
    private fun pushWindowInsets(navigationBarDp: Int, statusBarDp: Int) {
        WindowCompat.setDecorFitsSystemWindows(compose.activity.window, false)
        val status = dpPx(statusBarDp)
        val nav = dpPx(navigationBarDp)
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, status, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, nav))
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, status, 0, nav))
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.statusBars(),
                Insets.of(0, status, 0, 0),
            )
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(0, 0, 0, nav),
            )
            .setVisible(WindowInsetsCompat.Type.statusBars(), true)
            .setVisible(WindowInsetsCompat.Type.navigationBars(), true)
            .build()

        val holderClass = Class.forName("androidx.compose.foundation.layout.WindowInsetsHolder")
        val companion = holderClass.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
        val companionClass = companion.javaClass
        companionClass.methods.firstOrNull { it.name == "setUseTestInsets" }?.apply {
            isAccessible = true
            invoke(companion, true)
        }
        val viewMapField = (
            companionClass.declaredFields.asList() + holderClass.declaredFields.asList()
            ).firstOrNull { it.name.startsWith("viewMap") }
            ?: error(
                "holder=${holderClass.name} fields=${holderClass.declaredFields.joinToString { it.name }} " +
                    "companion=${companionClass.name} fields=${companionClass.declaredFields.joinToString { it.name }} " +
                    "methods=${companionClass.declaredMethods.joinToString { it.name }}",
            )
        viewMapField.isAccessible = true
        val holders = (viewMapField.get(companion) as Map<*, *>).values.filterNotNull()
        check(holders.isNotEmpty()) { "Inbox did not read window insets, so nothing can receive the nav bar" }
        val update = holders.first().javaClass.methods.first { method ->
            method.name == "update" && method.parameterTypes.size == 2
        }
        holders.forEach { holder -> update.invoke(holder, insets, 0) }

        // Keep a later requestApplyInsets from replacing the simulated bar with Robolectric's empty one.
        val decor = compose.activity.window.decorView
        fun hold(view: View) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, _ -> insets }
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) hold(view.getChildAt(index))
            }
        }
        hold(decor)
    }

    private fun dpPx(dp: Int): Int =
        (dp * compose.activity.resources.displayMetrics.density).toInt()

    private fun assertClearsNavigationBar(bitmap: Bitmap, navigationBarDp: Int, surface: Int) {
        val gap = bottomClearancePx(bitmap, surface)
        val inset = dpPx(navigationBarDp)
        check(gap >= inset) {
            "3-button navigation bar (${inset}px) still covers the toolbar (gap=${gap}px)"
        }
    }

    /** Distance from the bottom edge to the first row that is not the scaffold surface. */
    private fun bottomClearancePx(bitmap: Bitmap, surface: Int): Int {
        val step = 8
        for (y in bitmap.height - 1 downTo 0) {
            var samples = 0
            var different = 0
            var x = 0
            while (x < bitmap.width) {
                samples++
                if (!near(bitmap.getPixel(x, y), surface)) different++
                x += step
            }
            if (samples > 0 && different * 10 > samples) return bitmap.height - 1 - y
        }
        return bitmap.height
    }

    private fun near(pixel: Int, expected: Int): Boolean {
        val dr = abs((pixel shr 16 and 0xFF) - (expected shr 16 and 0xFF))
        val dg = abs((pixel shr 8 and 0xFF) - (expected shr 8 and 0xFF))
        val db = abs((pixel and 0xFF) - (expected and 0xFF))
        return dr + dg + db < 36
    }

    /** Draws a 3-button bar into the empty inset so the screenshot shows what the system covers. */
    private fun overlayNavigationBar(bitmap: Bitmap): Bitmap {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val nav = dpPx(48)
        val canvas = Canvas(copy)
        val top = (copy.height - nav).toFloat()
        val bar = Paint().apply { color = Color.argb(236, 18, 18, 18) }
        canvas.drawRect(0f, top, copy.width.toFloat(), copy.height.toFloat(), bar)
        val button = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpPx(2).toFloat().coerceAtLeast(2f)
            isAntiAlias = true
        }
        val cy = top + nav / 2f
        val positions = listOf(0.25f, 0.5f, 0.75f)
        positions.forEachIndexed { index, fraction ->
            val cx = copy.width * fraction
            val radius = nav * 0.18f
            when (index) {
                0 -> canvas.drawPath(triangle(cx, cy, radius), button)
                1 -> canvas.drawCircle(cx, cy, radius, button)
                else -> canvas.drawRoundRect(
                    cx - radius,
                    cy - radius * 0.7f,
                    cx + radius,
                    cy + radius * 0.7f,
                    6f,
                    6f,
                    button,
                )
            }
        }
        return copy
    }

    private fun triangle(cx: Float, cy: Float, radius: Float): android.graphics.Path {
        val path = android.graphics.Path()
        path.moveTo(cx - radius, cy)
        path.lineTo(cx + radius * 0.6f, cy - radius)
        path.lineTo(cx + radius * 0.6f, cy + radius)
        path.close()
        return path
    }

    private fun draw(window: Window): Bitmap {
        val view = window.decorView
        val metrics = view.resources.displayMetrics
        val width = view.width.takeIf { it > 0 } ?: metrics.widthPixels
        val height = view.height.takeIf { it > 0 } ?: metrics.heightPixels
        if (view.width == 0 || view.height == 0) {
            view.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun coloredPixels(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { pixel ->
            val alpha = pixel ushr 24
            alpha != 0 && pixel != 0xFFFFFFFF.toInt()
        }
    }
}
