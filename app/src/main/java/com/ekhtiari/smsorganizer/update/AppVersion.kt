package com.ekhtiari.smsorganizer.update

/**
 * Semantic version (major.minor.patch with an optional pre-release).
 * Build metadata after '+' is ignored. A leading "v" is accepted.
 *
 * The Gradle debug build type appends "-debug" to [versionName]. That suffix is a
 * build type, not a published pre-release, so [isNewerRelease] strips it from the
 * installed version before comparing.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
) : Comparable<AppVersion> {

    val baseVersion: String
        get() = "$major.$minor.$patch"

    fun canonical(): String = if (preRelease.isEmpty()) {
        baseVersion
    } else {
        "$baseVersion-${preRelease.joinToString(".")}"
    }

    override fun compareTo(other: AppVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        return comparePreRelease(preRelease, other.preRelease)
    }

    companion object {
        private val PATTERN = Regex(
            pattern = """^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+[0-9A-Za-z.-]+)?$""",
            option = RegexOption.IGNORE_CASE,
        )
        private val DEBUG_SUFFIX = Regex("""-debug$""", RegexOption.IGNORE_CASE)

        fun parse(raw: String?): AppVersion? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            val match = PATTERN.matchEntire(trimmed) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: 0
            val patch = match.groupValues[3].toIntOrNull() ?: 0
            val preRaw = match.groupValues[4]
            val preRelease = if (preRaw.isEmpty()) {
                emptyList()
            } else {
                preRaw.split('.')
            }
            if (preRelease.any { it.isEmpty() }) return null
            return AppVersion(major, minor, patch, preRelease)
        }

        /** Removes the local debug build-type suffix, leaving a published version string. */
        fun stripDebugSuffix(versionName: String): String {
            val trimmed = versionName.trim()
            val stripped = DEBUG_SUFFIX.replace(trimmed, "")
            return stripped.ifBlank { trimmed }
        }
    }
}

/**
 * True when [remoteVersion] (a tag such as "v1.0.7") is strictly newer than the
 * installed versionName. Returns false when either side cannot be parsed.
 */
fun isNewerRelease(installedVersionName: String, remoteVersion: String): Boolean {
    val installed = AppVersion.parse(AppVersion.stripDebugSuffix(installedVersionName)) ?: return false
    val remote = AppVersion.parse(remoteVersion) ?: return false
    return remote > installed
}

private fun comparePreRelease(left: List<String>, right: List<String>): Int {
    if (left.isEmpty() && right.isEmpty()) return 0
    // A release build sorts after any pre-release of the same numbers.
    if (left.isEmpty()) return 1
    if (right.isEmpty()) return -1
    val count = minOf(left.size, right.size)
    for (index in 0 until count) {
        val compared = compareIdentifier(left[index], right[index])
        if (compared != 0) return compared
    }
    return left.size.compareTo(right.size)
}

private fun compareIdentifier(left: String, right: String): Int {
    val leftNumber = numericIdentifier(left)
    val rightNumber = numericIdentifier(right)
    return when {
        leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
        leftNumber != null -> -1
        rightNumber != null -> 1
        else -> left.compareTo(right)
    }
}

/** SemVer numeric identifiers cannot have leading zeros. Those compare as text. */
private fun numericIdentifier(value: String): Int? {
    if (value.length > 1 && value.startsWith('0')) return null
    return value.toIntOrNull()
}
