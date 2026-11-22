package com.ekhtiari.smsorganizer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseParserTest {

    @Test
    fun parsesLatestReleaseAndSkipsDebugApk() {
        val release = GithubReleaseParser.parse(
            """
            {
              "html_url": "https://github.com/ekhtiari/smsorganizer/releases/tag/v1.0.7",
              "tag_name": "v1.0.7",
              "author": { "html_url": "https://github.com/ekhtiari" },
              "body": "Notes mentioning tag_name and \"quotes\"",
              "assets": [
                {
                  "name": "SMS-Organizer-1.0.7-debug.apk",
                  "browser_download_url": "https://github.com/ekhtiari/smsorganizer/releases/download/v1.0.7/SMS-Organizer-1.0.7-debug.apk"
                },
                {
                  "name": "SMS-Organizer-1.0.7.apk",
                  "browser_download_url": "https://github.com/ekhtiari/smsorganizer/releases/download/v1.0.7/SMS-Organizer-1.0.7.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("1.0.7", release!!.version.canonical())
        assertTrue(release.downloadUrl.endsWith("/SMS-Organizer-1.0.7.apk"))
    }

    @Test
    fun usesReleasePageWhenTheOnlyApkIsDebug() {
        val release = GithubReleaseParser.parse(
            """
            {
              "tag_name": "v1.0.8",
              "html_url": "https://github.com/ekhtiari/smsorganizer/releases/tag/v1.0.8",
              "assets": [
                {
                  "name": "SMS-Organizer-1.0.8-debug.apk",
                  "browser_download_url": "https://github.com/ekhtiari/smsorganizer/releases/download/v1.0.8/SMS-Organizer-1.0.8-debug.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            "https://github.com/ekhtiari/smsorganizer/releases/tag/v1.0.8",
            release!!.downloadUrl,
        )
    }

    @Test
    fun acceptsARenamedReleaseApk() {
        val release = GithubReleaseParser.parse(
            """
            {
              "tag_name": "v1.0.9",
              "html_url": "https://github.com/ekhtiari/smsorganizer/releases/tag/v1.0.9",
              "assets": [
                {
                  "name": "SMS-Organizer-1.0.9-signed.apk",
                  "browser_download_url": "https://github.com/ekhtiari/smsorganizer/releases/download/v1.0.9/SMS-Organizer-1.0.9-signed.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertTrue(release!!.downloadUrl.endsWith("SMS-Organizer-1.0.9-signed.apk"))
    }

    @Test
    fun returnsNullForMalformedOrUntaggedPayloads() {
        assertNull(GithubReleaseParser.parse(""))
        assertNull(GithubReleaseParser.parse("{"))
        assertNull(GithubReleaseParser.parse("""{"html_url":"https://github.com/ekhtiari"}"""))
        assertNull(GithubReleaseParser.parse("""{"tag_name":"nightly"}"""))
    }
}
