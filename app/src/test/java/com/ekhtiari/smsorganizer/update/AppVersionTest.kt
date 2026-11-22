package com.ekhtiari.smsorganizer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun parsesReleaseTags() {
        assertEquals(AppVersion(1, 0, 6), AppVersion.parse("1.0.6"))
        assertEquals(AppVersion(1, 0, 6), AppVersion.parse("v1.0.6"))
        assertEquals(AppVersion(1, 0, 6), AppVersion.parse("V1.0.6"))
        assertEquals(AppVersion(1, 2, 0), AppVersion.parse("v1.2"))
        assertEquals(AppVersion(1, 0, 0), AppVersion.parse("1"))
        assertEquals(AppVersion(1, 0, 6), AppVersion.parse("  v1.0.6  "))
    }

    @Test
    fun ignoresBuildMetadata() {
        assertEquals(AppVersion(1, 0, 6), AppVersion.parse("1.0.6+3"))
        assertEquals(AppVersion(1, 0, 6), AppVersion.parse("v1.0.6+build.12"))
        assertEquals(0, AppVersion.parse("1.0.6+1")!!.compareTo(AppVersion.parse("1.0.6+2")!!))
    }

    @Test
    fun rejectsUnusableStrings() {
        assertNull(AppVersion.parse(null))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("   "))
        assertNull(AppVersion.parse("abc"))
        assertNull(AppVersion.parse("v"))
        assertNull(AppVersion.parse("1.0.6-"))
        assertNull(AppVersion.parse("1.0.6."))
        assertNull(AppVersion.parse("1.0.0.1"))
    }

    @Test
    fun comparesNumericallyNotLexically() {
        assertTrue(AppVersion.parse("1.0.10")!! > AppVersion.parse("1.0.9")!!)
        assertTrue(AppVersion.parse("1.10.0")!! > AppVersion.parse("1.9.0")!!)
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.9.9")!!)
        assertEquals(0, AppVersion.parse("v1.2.0")!!.compareTo(AppVersion.parse("1.2")!!))
    }

    @Test
    fun preReleaseSortsBeforeReleaseAndByIdentifier() {
        val alpha = AppVersion.parse("1.0.0-alpha")!!
        val alpha1 = AppVersion.parse("1.0.0-alpha.1")!!
        val alphaBeta = AppVersion.parse("1.0.0-alpha.beta")!!
        val beta = AppVersion.parse("1.0.0-beta")!!
        val beta2 = AppVersion.parse("1.0.0-beta.2")!!
        val beta11 = AppVersion.parse("1.0.0-beta.11")!!
        val rc = AppVersion.parse("1.0.0-rc.1")!!
        val release = AppVersion.parse("1.0.0")!!

        assertTrue(alpha < alpha1)
        assertTrue(alpha1 < alphaBeta)
        assertTrue(alphaBeta < beta)
        assertTrue(beta < beta2)
        assertTrue(beta2 < beta11)
        assertTrue(beta11 < rc)
        assertTrue(rc < release)
    }

    @Test
    fun numericIdentifiersWithLeadingZerosCompareAsText() {
        val numeric = AppVersion.parse("1.0.0-alpha.1")!!
        val leadingZero = AppVersion.parse("1.0.0-alpha.01")!!
        assertTrue(numeric < leadingZero)
    }

    @Test
    fun stripsDebugBuildSuffixOnly() {
        assertEquals("1.0.6", AppVersion.stripDebugSuffix("1.0.6-debug"))
        assertEquals("1.0.6", AppVersion.stripDebugSuffix("1.0.6-DEBUG"))
        assertEquals("1.0.6", AppVersion.stripDebugSuffix("  1.0.6-debug  "))
        assertEquals("1.0.6-rc", AppVersion.stripDebugSuffix("1.0.6-rc"))
    }

    @Test
    fun installedDebugBuildIsNotNewerThanTheSameRelease() {
        assertFalse(isNewerRelease("1.0.6-debug", "v1.0.6"))
        assertFalse(isNewerRelease("1.0.6", "v1.0.6"))
        assertFalse(isNewerRelease("1.0.6", "1.0.5"))
        assertFalse(isNewerRelease("1.0.6", "v1.0.6-rc.2"))
    }

    @Test
    fun remoteReleaseIsNewer() {
        assertTrue(isNewerRelease("1.0.5", "v1.0.6"))
        assertTrue(isNewerRelease("1.0.6-debug", "v1.0.7"))
        assertTrue(isNewerRelease("1.0.6-rc.1", "v1.0.6"))
        assertTrue(isNewerRelease("1.9.9", "v2.0.0"))
    }

    @Test
    fun unparsableVersionsAreNotNewer() {
        assertFalse(isNewerRelease("not-a-version", "v1.0.7"))
        assertFalse(isNewerRelease("1.0.6", "latest"))
        assertFalse(isNewerRelease("", ""))
    }
}
