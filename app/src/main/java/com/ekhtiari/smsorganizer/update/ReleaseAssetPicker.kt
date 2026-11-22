package com.ekhtiari.smsorganizer.update

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
)

object UpdateLinks {
    const val LATEST_API = "https://api.github.com/repos/ekhtiari/smsorganizer/releases/latest"
    const val LATEST_PAGE = "https://github.com/ekhtiari/smsorganizer/releases/latest"
}

/**
 * Picks a download URL from a GitHub release.
 *
 * Asset file names can change when signing changes, so this matches by shape:
 * an https link ending in ".apk" whose name does not contain "debug".
 * A file named SMS-Organizer-version.apk wins, then any other name containing the
 * version, preferring a name that is not marked unsigned.
 * With no release APK, the release page is returned so the browser can still open it.
 */
object ReleaseAssetPicker {
    fun pick(version: AppVersion, assets: List<ReleaseAsset>, releasePageUrl: String): String {
        val releaseApks = assets.filter { it.isReleaseApk() }
        if (releaseApks.isEmpty()) return httpsPage(releasePageUrl)
        val exactName = "SMS-Organizer-${version.baseVersion}.apk"
        prefer(releaseApks.filter { it.name.equals(exactName, ignoreCase = true) })
            ?.let { return it.downloadUrl }
        prefer(releaseApks.filter { it.name.contains(version.baseVersion, ignoreCase = true) })
            ?.let { return it.downloadUrl }
        return prefer(releaseApks)!!.downloadUrl
    }

    private fun prefer(candidates: List<ReleaseAsset>): ReleaseAsset? {
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { !it.name.contains("unsigned", ignoreCase = true) }
            ?: candidates.first()
    }

    private fun httpsPage(releasePageUrl: String): String {
        val trimmed = releasePageUrl.trim()
        return if (trimmed.startsWith("https://")) trimmed else UpdateLinks.LATEST_PAGE
    }

    private fun ReleaseAsset.isReleaseApk(): Boolean {
        if (!downloadUrl.startsWith("https://")) return false
        if (!name.endsWith(".apk", ignoreCase = true)) return false
        if (name.contains("debug", ignoreCase = true)) return false
        return true
    }
}
