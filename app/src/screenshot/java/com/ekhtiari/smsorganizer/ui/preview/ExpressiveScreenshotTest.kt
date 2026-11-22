package com.ekhtiari.smsorganizer.ui.preview

import android.graphics.Bitmap
import android.os.Looper
import android.view.View.MeasureSpec
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
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

    private fun capture(name: String, show: () -> Unit) {
        show()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        val dialog = ShadowDialog.getLatestDialog()
        val window = if (dialog?.isShowing == true) {
            dialog.window ?: compose.activity.window
        } else {
            compose.activity.window
        }
        val bitmap = draw(window)
        val dir = File("/opt/cursor/artifacts")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        check(coloredPixels(bitmap) > 500) { "$name rendered almost blank" }
        if (dialog?.isShowing == true) dialog.dismiss()
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
