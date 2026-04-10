package io.github.c1921.mosswriter.data.remote

import android.util.Base64
import android.util.Xml
import io.github.c1921.mosswriter.data.model.WebDavConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RemoteFile(val name: String, val lastModified: Long)

class WebDavClient(private val config: WebDavConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val authHeader: String
        get() {
            val credentials = "${config.username}:${config.password}"
            return "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }

    // 项目级路径：{rootUrl}/MossWriter/{projectName}/
    private val projectUrl: String get() = config.projectUrl

    // 根 URL（用于创建 MossWriter/ 父目录）
    private val mossWriterUrl: String
        get() = config.url.trimEnd('/') + "/MossWriter/"

    fun testConnection(): Result<Unit> = runCatching {
        ensureProjectDirs()
    }

    fun listRemoteFiles(): Result<List<RemoteFile>> = runCatching {
        val propfindBody = PROPFIND_BODY.toRequestBody("application/xml".toMediaType())
        val request = Request.Builder()
            .url(projectUrl)
            .method("PROPFIND", propfindBody)
            .header("Authorization", authHeader)
            .header("Depth", "1")
            .header("Content-Type", "application/xml")
            .build()
        val responseBody = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 207) {
                throw Exception("PROPFIND failed: HTTP ${response.code}")
            }
            response.body?.string() ?: throw Exception("Empty response")
        }
        parseMultistatus(responseBody)
    }

    fun downloadFile(name: String): Result<String> = runCatching {
        val request = Request.Builder()
            .url(projectUrl + name)
            .get()
            .header("Authorization", authHeader)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("GET failed: HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }

    fun uploadFile(name: String, content: String): Result<Unit> = runCatching {
        val body = content.toRequestBody("text/plain; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(projectUrl + name)
            .put(body)
            .header("Authorization", authHeader)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("PUT failed: HTTP ${response.code}")
        }
    }

    fun deleteRemoteFile(name: String): Result<Unit> = runCatching {
        val request = Request.Builder()
            .url(projectUrl + name)
            .delete()
            .header("Authorization", authHeader)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw Exception("DELETE failed: HTTP ${response.code}")
            }
        }
    }

    // 确保 MossWriter/ 和 MossWriter/{projectName}/ 目录存在，不存在则 MKCOL 创建
    private fun ensureProjectDirs() {
        ensureDir(mossWriterUrl)
        ensureDir(projectUrl)
    }

    private fun ensureDir(url: String) {
        val checkRequest = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_BODY.toRequestBody("application/xml".toMediaType()))
            .header("Authorization", authHeader)
            .header("Depth", "0")
            .header("Content-Type", "application/xml")
            .build()
        val code = client.newCall(checkRequest).execute().use { it.code }
        if (code == 404 || code == 409) {
            val mkcolRequest = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .header("Authorization", authHeader)
                .build()
            client.newCall(mkcolRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 405) {
                    // 405 = already exists on some servers
                    throw Exception("MKCOL failed: HTTP ${response.code}")
                }
            }
        } else if (code != 207 && code !in 200..299) {
            throw Exception("Cannot access directory: HTTP $code")
        }
    }

    private fun parseMultistatus(xml: String): List<RemoteFile> {
        val results = mutableListOf<RemoteFile>()
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())

        var href = ""
        var lastModified = 0L
        var inResponse = false
        var inHref = false
        var inLastModified = false
        var isCollection = false

        while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    val localName = parser.name.substringAfterLast(':')
                    when (localName) {
                        "response" -> { inResponse = true; href = ""; lastModified = 0L; isCollection = false }
                        "href" -> inHref = true
                        "getlastmodified" -> inLastModified = true
                        "collection" -> isCollection = true
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (inHref) href = parser.text.trim()
                    if (inLastModified) lastModified = parseHttpDate(parser.text.trim())
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    val localName = parser.name.substringAfterLast(':')
                    when (localName) {
                        "href" -> inHref = false
                        "getlastmodified" -> inLastModified = false
                        "response" -> {
                            if (inResponse && !isCollection) {
                                val fileName = href.substringAfterLast('/').let {
                                    java.net.URLDecoder.decode(it, "UTF-8")
                                }
                                if (fileName.endsWith(".md")) {
                                    results.add(RemoteFile(fileName, lastModified))
                                }
                            }
                            inResponse = false
                        }
                    }
                }
            }
            parser.next()
        }
        return results
    }

    private fun parseHttpDate(httpDate: String): Long {
        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US)
        )
        for (fmt in formats) {
            try { return fmt.parse(httpDate)?.time ?: 0L } catch (_: Exception) {}
        }
        return 0L
    }

    companion object {
        private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    <D:getlastmodified/>
    <D:resourcetype/>
  </D:prop>
</D:propfind>"""
    }
}
