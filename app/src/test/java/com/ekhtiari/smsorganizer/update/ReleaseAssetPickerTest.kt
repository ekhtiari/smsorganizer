package com.ekhtiari.smsorganizer.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseAssetPickerTest {

    private val version = AppVersion.parse("v1.0.7")!!
    private val page = "https://github.com/ekhtiari/smsorganizer/releases/tag/v1.0.7"

    @Test
    fun prefersNonDebugApkWhenDebugIsListedFirst() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(
                asset("SMS-Organizer-1.0.7-debug.apk", "https://example.com/debug.apk"),
                asset("SMS-Organizer-1.0.7.apk", "https://example.com/release.apk"),
            ),
            page,
        )
        assertEquals("https://example.com/release.apk", picked)
    }

    @Test
    fun prefersExactReleaseNameOverRenamedSibling() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(
                asset("extra-1.0.7.apk", "https://example.com/extra.apk"),
                asset("sms-organizer-1.0.7.APK", "https://example.com/exact.apk"),
            ),
            page,
        )
        assertEquals("https://example.com/exact.apk", picked)
    }

    @Test
    fun acceptsRenamedNonDebugApk() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(
                asset("SMS-Organizer-1.0.7-debug.apk", "https://example.com/debug.apk"),
                asset("SMS-Organizer-1.0.7-signed.apk", "https://example.com/signed.apk"),
            ),
            page,
        )
        assertEquals("https://example.com/signed.apk", picked)
    }

    @Test
    fun prefersSignedNameOverUnsigned() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(
                asset("SMS-Organizer-1.0.7-unsigned.apk", "https://example.com/unsigned.apk"),
                asset("SMS-Organizer-1.0.7-signed.apk", "https://example.com/signed.apk"),
            ),
            page,
        )
        assertEquals("https://example.com/signed.apk", picked)
    }

    @Test
    fun usesUnsignedApkWhenItIsTheOnlyReleaseAsset() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(asset("app-release-unsigned.apk", "https://example.com/unsigned.apk")),
            page,
        )
        assertEquals("https://example.com/unsigned.apk", picked)
    }

    @Test
    fun fallsBackToReleasePageWhenOnlyDebugApkExists() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(asset("SMS-Organizer-1.0.7-debug.apk", "https://example.com/debug.apk")),
            page,
        )
        assertEquals(page, picked)
    }

    @Test
    fun fallsBackToReleasePageWhenNoApkIsAttached() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(asset("notes.txt", "https://example.com/notes.txt")),
            page,
        )
        assertEquals(page, picked)
    }

    @Test
    fun ignoresInsecureDownloadUrls() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(asset("SMS-Organizer-1.0.7.apk", "http://example.com/app.apk")),
            page,
        )
        assertEquals(page, picked)
    }

    @Test
    fun fallsBackToLatestPageWhenReleaseUrlIsMissing() {
        val picked = ReleaseAssetPicker.pick(version, emptyList(), "http://insecure.example/rel")
        assertEquals(UpdateLinks.LATEST_PAGE, picked)
    }

    @Test
    fun ignoresNamesThatOnlyLookSimilarToApk() {
        val picked = ReleaseAssetPicker.pick(
            version,
            listOf(
                asset("SMS-Organizer-1.0.7.apks", "https://example.com/bundle.apks"),
                asset("SMS-Organizer-1.0.7.apk.txt", "https://example.com/notes.apk.txt"),
            ),
            page,
        )
        assertEquals(page, picked)
    }

    private fun asset(name: String, url: String) = ReleaseAsset(name, url)
}
