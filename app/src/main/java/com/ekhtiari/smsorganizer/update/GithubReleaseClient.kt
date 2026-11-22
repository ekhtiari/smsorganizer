package com.ekhtiari.smsorganizer.update

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GithubRelease(
    val version: AppVersion,
    val downloadUrl: String,
)

fun interface ReleaseSource {
    /** Latest stable release, or null when the request fails or the body is unusable. */
    fun fetchLatest(): GithubRelease?
}

object GithubReleaseParser {
    fun parse(json: String): GithubRelease? {
        val root = JsonValues.parseObject(json) ?: return null
        val version = AppVersion.parse(root.string("tag_name")) ?: return null
        val assets = root.assets()
        val downloadUrl = ReleaseAssetPicker.pick(
            version = version,
            assets = assets,
            releasePageUrl = root.string("html_url"),
        )
        return GithubRelease(version = version, downloadUrl = downloadUrl)
    }

    private fun Map<String, Any?>.string(key: String): String = this[key] as? String ?: ""

    private fun Map<String, Any?>.assets(): List<ReleaseAsset> {
        val array = this["assets"] as? List<*> ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? Map<*, *> ?: return@mapNotNull null
            val name = obj["name"] as? String ?: return@mapNotNull null
            val url = obj["browser_download_url"] as? String ?: return@mapNotNull null
            if (name.isBlank() || url.isBlank()) null else ReleaseAsset(name, url)
        }
    }
}

/** JSON object reader for the release payload. Avoids android.jar's org.json stub in unit tests. */
internal object JsonValues {
    fun parseObject(json: String): Map<String, Any?>? {
        if (json.isBlank()) return null
        return try {
            val reader = JsonReader(json)
            val value = reader.parseValue() as? Map<*, *> ?: return null
            reader.finish()
            value.entries.associate { (key, entry) -> key.toString() to entry }
        } catch (_: Exception) {
            null
        }
    }
}

private class JsonReader(private val text: String) {
    private var index = 0

    fun parseValue(): Any? {
        skipWhitespace()
        if (index >= text.length) error("Unexpected end of JSON")
        return when (val char = text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> error("Unexpected '$char'")
        }
    }

    fun finish() {
        skipWhitespace()
        if (index != text.length) error("Trailing JSON")
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        skipWhitespace()
        if (consume('}')) return emptyMap()
        val values = linkedMapOf<String, Any?>()
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            values[key] = parseValue()
            skipWhitespace()
            when {
                consume(',') -> continue
                consume('}') -> return values
                else -> error("Expected ',' or '}'")
            }
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        skipWhitespace()
        if (consume(']')) return emptyList()
        val values = mutableListOf<Any?>()
        while (true) {
            values += parseValue()
            skipWhitespace()
            when {
                consume(',') -> continue
                consume(']') -> return values
                else -> error("Expected ',' or ']'")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val out = StringBuilder()
        while (index < text.length) {
            when (val char = text[index++]) {
                '"' -> return out.toString()
                '\\' -> out.append(escape())
                else -> out.append(char)
            }
        }
        error("Unterminated string")
    }

    private fun escape(): Char {
        if (index >= text.length) error("Bad escape")
        return when (val char = text[index++]) {
            '"', '\\', '/' -> char
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (index + 4 > text.length) error("Bad unicode escape")
                val hex = text.substring(index, index + 4)
                index += 4
                hex.toInt(16).toChar()
            }
            else -> error("Bad escape")
        }
    }

    private fun parseNumber(): String {
        val start = index
        consume('-')
        if (index >= text.length || text[index] !in '0'..'9') error("Bad number")
        while (index < text.length && text[index] in '0'..'9') index++
        if (consume('.')) {
            if (index >= text.length || text[index] !in '0'..'9') error("Bad number")
            while (index < text.length && text[index] in '0'..'9') index++
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            if (index >= text.length || text[index] !in '0'..'9') error("Bad number")
            while (index < text.length && text[index] in '0'..'9') index++
        }
        return text.substring(start, index)
    }

    private fun literal(token: String, value: Any?): Any? {
        if (!text.startsWith(token, index)) error("Expected $token")
        index += token.length
        return value
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun expect(char: Char) {
        if (!consume(char)) error("Expected '$char'")
    }

    private fun consume(char: Char): Boolean {
        if (index < text.length && text[index] == char) {
            index++
            return true
        }
        return false
    }
}

/**
 * Unauthenticated read of the GitHub latest-release endpoint.
 * Timeouts are short because GitHub is often slow or blocked; any failure returns null.
 */
class GithubReleaseClient(
    private val endpoint: String = UpdateLinks.LATEST_API,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
) : ReleaseSource {

    override fun fetchLatest(): GithubRelease? {
        return try {
            val body = download() ?: return null
            GithubReleaseParser.parse(body)
        } catch (_: Exception) {
            null
        }
    }

    private fun download(): String? {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SMS-Organizer")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()
                return null
            }
            connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BODY_BYTES) return null
                    out.write(buffer, 0, read)
                }
                return out.toString(Charsets.UTF_8.name())
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 4_000
        const val READ_TIMEOUT_MS = 5_000
        private const val MAX_BODY_BYTES = 1_048_576
    }
}
