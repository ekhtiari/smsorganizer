package com.ekhtiari.smsorganizer.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UpdateOffer(
    val versionName: String,
    val downloadUrl: String,
) {
    /** True when the URL points at an APK rather than the release page. */
    val directApk: Boolean
        get() = downloadUrl.substringBefore('?').endsWith(".apk", ignoreCase = true)
}

sealed class UpdateCheckResult {
    data class Available(val offer: UpdateOffer) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data object Failed : UpdateCheckResult()
    data object Skipped : UpdateCheckResult()
}

interface UpdateStore {
    var lastCheckAtMillis: Long
    var lastCheckSucceeded: Boolean
    var cachedVersion: String?
    var cachedDownloadUrl: String?
    var dismissedVersion: String?
    var notifiedVersion: String?

    /** Writes the cached release in one step so the version and URL cannot diverge. */
    fun putCachedOffer(version: String?, downloadUrl: String?) {
        cachedVersion = version
        cachedDownloadUrl = downloadUrl
    }
}

fun UpdateStore.clearCachedOffer() {
    putCachedOffer(null, null)
}

interface UpdateNotifier {
    /** @return true when the notification was posted. */
    fun show(offer: UpdateOffer): Boolean
    fun cancel()
}

/**
 * Checks GitHub for a newer release without blocking app startup.
 *
 * Call [check] from a background thread. Network failures are recorded and retried
 * later; they never throw. Automatic checks run at most once per [SUCCESS_INTERVAL_MS]
 * after a success, or [FAILURE_INTERVAL_MS] after a failure. "Later" stores the
 * dismissed version so that release is not prompted again. A newer tag still shows.
 */
class UpdateChecker(
    private val store: UpdateStore,
    private val releases: ReleaseSource,
    private val notifier: UpdateNotifier,
    private val installedVersionName: String,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private val _offer = MutableStateFlow<UpdateOffer?>(null)
    val offer: StateFlow<UpdateOffer?> = _offer.asStateFlow()

    /** Version hidden by dismissing the dialog without tapping Later. Reset on process death. */
    private var sessionHiddenVersion: String? = null

    fun check(force: Boolean): UpdateCheckResult {
        val fetch = synchronized(lock) {
            publish(readPendingOffer(), force)
            force || shouldFetch(clock())
        }
        if (!fetch) return UpdateCheckResult.Skipped

        val release = try {
            releases.fetchLatest()
        } catch (_: Exception) {
            null
        }

        return synchronized(lock) {
            val now = clock()
            if (release == null) {
                store.lastCheckAtMillis = now
                store.lastCheckSucceeded = false
                UpdateCheckResult.Failed
            } else {
                applyRelease(release, force, now)
            }
        }
    }

    /** Permanently hides prompts for the current offered version. */
    fun dismissCurrent() = synchronized(lock) {
        val version = _offer.value?.versionName ?: store.cachedVersion
        if (!version.isNullOrBlank()) {
            store.dismissedVersion = AppVersion.parse(version)?.canonical() ?: version
        }
        sessionHiddenVersion = store.dismissedVersion
        notifier.cancel()
        _offer.value = null
    }

    /** Hides the in-app prompt until the next process start or an explicit check. */
    fun hideForSession() = synchronized(lock) {
        sessionHiddenVersion = _offer.value?.versionName ?: store.cachedVersion
        _offer.value = null
    }

    private fun applyRelease(release: GithubRelease, force: Boolean, now: Long): UpdateCheckResult {
        val versionName = release.version.canonical()
        if (!isNewerRelease(installedVersionName, versionName)) {
            store.clearCachedOffer()
            notifier.cancel()
            _offer.value = null
            store.lastCheckAtMillis = now
            store.lastCheckSucceeded = true
            return UpdateCheckResult.UpToDate
        }
        val downloadUrl = release.downloadUrl.takeIf { it.startsWith("https://") }
            ?: UpdateLinks.LATEST_PAGE
        val offer = UpdateOffer(versionName = versionName, downloadUrl = downloadUrl)
        store.putCachedOffer(offer.versionName, offer.downloadUrl)
        store.lastCheckAtMillis = now
        store.lastCheckSucceeded = true
        publish(offer, force)
        return UpdateCheckResult.Available(offer)
    }

    private fun publish(offer: UpdateOffer?, force: Boolean) {
        if (offer == null) return
        val dismissed = store.dismissedVersion == offer.versionName
        if (!dismissed) maybeNotify(offer)
        val suppressDialog = !force && (dismissed || sessionHiddenVersion == offer.versionName)
        _offer.value = if (suppressDialog) null else offer
    }

    private fun maybeNotify(offer: UpdateOffer) {
        if (store.notifiedVersion == offer.versionName) return
        if (notifier.show(offer)) {
            store.notifiedVersion = offer.versionName
        }
    }

    private fun readPendingOffer(): UpdateOffer? {
        val versionName = store.cachedVersion
        val url = store.cachedDownloadUrl
        if (versionName.isNullOrBlank() || url.isNullOrBlank() || !url.startsWith("https://")) {
            if (!versionName.isNullOrBlank() || !url.isNullOrBlank()) store.clearCachedOffer()
            return null
        }
        val parsed = AppVersion.parse(versionName)
        if (parsed == null || !isNewerRelease(installedVersionName, parsed.canonical())) {
            store.clearCachedOffer()
            notifier.cancel()
            _offer.value = null
            return null
        }
        return UpdateOffer(versionName = parsed.canonical(), downloadUrl = url)
    }

    private fun shouldFetch(now: Long): Boolean {
        val last = store.lastCheckAtMillis
        if (last == 0L || now < last) return true
        val interval = if (store.lastCheckSucceeded) SUCCESS_INTERVAL_MS else FAILURE_INTERVAL_MS
        return now - last >= interval
    }

    companion object {
        const val SUCCESS_INTERVAL_MS = 6L * 60L * 60L * 1000L
        const val FAILURE_INTERVAL_MS = 60L * 60L * 1000L
    }
}
