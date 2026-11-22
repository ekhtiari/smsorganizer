package com.ekhtiari.smsorganizer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun newerReleaseIsOfferedAndNotifiedOnce() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")

        val result = fixture.checker.check(force = false)

        assertEquals(UpdateCheckResult.Available(offer("1.0.7")), result)
        assertEquals(offer("1.0.7"), fixture.checker.offer.value)
        assertEquals(listOf("1.0.7"), fixture.notifier.shown)
        assertEquals(1, fixture.releases.fetches)

        fixture.now += UpdateChecker.SUCCESS_INTERVAL_MS
        fixture.checker.check(force = false)

        assertEquals(listOf("1.0.7"), fixture.notifier.shown)
        assertEquals(2, fixture.releases.fetches)
    }

    @Test
    fun sameVersionAndDebugSuffixAreUpToDate() {
        val fixture = Fixture(installed = "1.0.6-debug")
        fixture.releases.next = release("1.0.6")

        assertEquals(UpdateCheckResult.UpToDate, fixture.checker.check(force = false))
        assertNull(fixture.checker.offer.value)
        assertTrue(fixture.notifier.shown.isEmpty())
    }

    @Test
    fun olderRemoteReleaseIsNotAnUpdate() {
        val fixture = Fixture(installed = "1.0.6")
        fixture.releases.next = release("1.0.5")

        assertEquals(UpdateCheckResult.UpToDate, fixture.checker.check(force = false))
        assertNull(fixture.checker.offer.value)
    }

    @Test
    fun dismissedVersionStaysQuietUntilANewerTag() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)
        fixture.checker.dismissCurrent()

        assertNull(fixture.checker.offer.value)
        assertEquals(1, fixture.notifier.cancelled)
        assertEquals("1.0.7", fixture.store.dismissedVersion)

        fixture.now += UpdateChecker.SUCCESS_INTERVAL_MS
        fixture.checker.check(force = false)

        assertNull(fixture.checker.offer.value)
        assertEquals(listOf("1.0.7"), fixture.notifier.shown)

        fixture.releases.next = release("1.0.8")
        fixture.now += UpdateChecker.SUCCESS_INTERVAL_MS
        fixture.checker.check(force = false)

        assertEquals(offer("1.0.8"), fixture.checker.offer.value)
        assertEquals(listOf("1.0.7", "1.0.8"), fixture.notifier.shown)
    }

    @Test
    fun manualCheckShowsADismissedUpdateWithoutNotifyingAgain() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)
        fixture.checker.dismissCurrent()

        fixture.now += 1_000
        val result = fixture.checker.check(force = true)

        assertEquals(UpdateCheckResult.Available(offer("1.0.7")), result)
        assertEquals(offer("1.0.7"), fixture.checker.offer.value)
        assertEquals(listOf("1.0.7"), fixture.notifier.shown)
    }

    @Test
    fun automaticCheckWaitsSeveralHours() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)

        fixture.now += UpdateChecker.SUCCESS_INTERVAL_MS - 1
        assertEquals(UpdateCheckResult.Skipped, fixture.checker.check(force = false))
        assertEquals(1, fixture.releases.fetches)
        assertEquals(offer("1.0.7"), fixture.checker.offer.value)

        fixture.now += 1
        fixture.checker.check(force = false)
        assertEquals(2, fixture.releases.fetches)
    }

    @Test
    fun manualCheckBypassesTheInterval() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)
        fixture.now += 1_000

        fixture.checker.check(force = true)

        assertEquals(2, fixture.releases.fetches)
    }

    @Test
    fun failureBacksOffAndKeepsAKnownUpdate() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)

        fixture.releases.fail = true
        fixture.now += UpdateChecker.SUCCESS_INTERVAL_MS
        assertEquals(UpdateCheckResult.Failed, fixture.checker.check(force = false))
        assertEquals(offer("1.0.7"), fixture.checker.offer.value)
        assertEquals(false, fixture.store.lastCheckSucceeded)

        val fetchesAfterFailure = fixture.releases.fetches
        fixture.now += UpdateChecker.FAILURE_INTERVAL_MS - 1
        assertEquals(UpdateCheckResult.Skipped, fixture.checker.check(force = false))
        assertEquals(fetchesAfterFailure, fixture.releases.fetches)

        fixture.now += 1
        fixture.checker.check(force = false)
        assertEquals(fetchesAfterFailure + 1, fixture.releases.fetches)
    }

    @Test
    fun failedCheckDoesNotNotifyAndRetriesAfterTheFailureInterval() {
        val fixture = Fixture()
        fixture.releases.fail = true

        assertEquals(UpdateCheckResult.Failed, fixture.checker.check(force = false))
        assertTrue(fixture.notifier.shown.isEmpty())
        assertNull(fixture.checker.offer.value)

        fixture.now += UpdateChecker.FAILURE_INTERVAL_MS - 1
        assertEquals(UpdateCheckResult.Skipped, fixture.checker.check(force = false))
        assertEquals(1, fixture.releases.fetches)
    }

    @Test
    fun clockGoingBackwardsDoesNotStickTheThrottle() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)
        fixture.now -= 10_000

        fixture.checker.check(force = false)

        assertEquals(2, fixture.releases.fetches)
    }

    @Test
    fun installingTheOfferedVersionClearsItWithoutAnotherDownload() {
        val fixture = Fixture(installed = "1.0.7")
        fixture.store.cachedVersion = "1.0.7"
        fixture.store.cachedDownloadUrl = "https://example.com/SMS-Organizer-1.0.7.apk"
        fixture.store.lastCheckAtMillis = fixture.now
        fixture.store.lastCheckSucceeded = true
        fixture.store.notifiedVersion = "1.0.7"

        assertEquals(UpdateCheckResult.Skipped, fixture.checker.check(force = false))
        assertNull(fixture.checker.offer.value)
        assertNull(fixture.store.cachedVersion)
        assertEquals(1, fixture.notifier.cancelled)
        assertEquals(0, fixture.releases.fetches)
    }

    @Test
    fun hideForSessionSuppressesThePromptUntilAManualCheck() {
        val fixture = Fixture()
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)
        fixture.checker.hideForSession()
        assertNull(fixture.checker.offer.value)

        fixture.now += UpdateChecker.SUCCESS_INTERVAL_MS
        fixture.checker.check(force = false)
        assertNull(fixture.checker.offer.value)

        fixture.checker.check(force = true)
        assertEquals(offer("1.0.7"), fixture.checker.offer.value)
    }

    @Test
    fun notificationIsRetriedWhenPostingWasNotAllowed() {
        val fixture = Fixture()
        fixture.notifier.posted = false
        fixture.releases.next = release("1.0.7")
        fixture.checker.check(force = false)

        assertNull(fixture.store.notifiedVersion)
        assertEquals(listOf("1.0.7"), fixture.notifier.shown)

        fixture.notifier.posted = true
        fixture.now += 1_000
        fixture.checker.check(force = false)

        assertEquals("1.0.7", fixture.store.notifiedVersion)
        assertEquals(listOf("1.0.7", "1.0.7"), fixture.notifier.shown)
        assertEquals(1, fixture.releases.fetches)
    }

    @Test
    fun directApkFlagFollowsTheUrl() {
        assertTrue(offer("1.0.7").directApk)
        assertEquals(
            false,
            UpdateOffer("1.0.7", UpdateLinks.LATEST_PAGE).directApk,
        )
    }

    private fun offer(version: String): UpdateOffer {
        return UpdateOffer(
            versionName = version,
            downloadUrl = "https://github.com/ekhtiari/smsorganizer/releases/download/v$version/SMS-Organizer-$version.apk",
        )
    }

    private fun release(version: String): GithubRelease {
        return GithubRelease(AppVersion.parse(version)!!, offer(version).downloadUrl)
    }

    private class Fixture(
        installed: String = "1.0.6",
        var now: Long = 1_000_000L,
    ) {
        val store = MemoryUpdateStore()
        val releases = FakeReleases()
        val notifier = RecordingNotifier()
        val checker = UpdateChecker(
            store = store,
            releases = releases,
            notifier = notifier,
            installedVersionName = installed,
            clock = { now },
        )
    }

    private class MemoryUpdateStore : UpdateStore {
        override var lastCheckAtMillis: Long = 0L
        override var lastCheckSucceeded: Boolean = false
        override var cachedVersion: String? = null
        override var cachedDownloadUrl: String? = null
        override var dismissedVersion: String? = null
        override var notifiedVersion: String? = null
    }

    private class FakeReleases : ReleaseSource {
        var next: GithubRelease? = null
        var fetches: Int = 0
        var fail: Boolean = false

        override fun fetchLatest(): GithubRelease? {
            fetches++
            if (fail) error("offline")
            return next
        }
    }

    private class RecordingNotifier : UpdateNotifier {
        val shown = mutableListOf<String>()
        var cancelled: Int = 0
        var posted: Boolean = true

        override fun show(offer: UpdateOffer): Boolean {
            shown += offer.versionName
            return posted
        }

        override fun cancel() {
            cancelled++
        }
    }
}
