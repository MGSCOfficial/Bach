package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val downloadUrl: String?,
    val isNewer: Boolean
)

class UpdateCheckerRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        const val CURRENT_VERSION = "1.6.4"
        private const val GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/MGSCOfficial/Bach/releases/latest"
    }

    suspend fun checkForUpdates(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Bach-Android/$CURRENT_VERSION")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return@withContext Result.success(null)
                    }
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val bodyStr = response.body?.string().orEmpty()
                if (bodyStr.isBlank()) return@withContext Result.success(null)

                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "").trim()
                val cleanVersion = tagName.removePrefix("v").removePrefix("V").trim()
                val releaseTitle = json.optString("name", "Bach $tagName")
                val releaseNotes = json.optString("body", "").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/MGSCOfficial/Bach/releases")

                var apkDownloadUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", null)
                            break
                        }
                    }
                }

                val isNewer = isVersionNewer(cleanVersion, CURRENT_VERSION)

                val info = AppUpdateInfo(
                    tagName = tagName,
                    versionName = cleanVersion,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    htmlUrl = htmlUrl,
                    downloadUrl = apkDownloadUrl ?: htmlUrl,
                    isNewer = isNewer
                )

                Result.success(info)
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error al verificar actualizaciones", e)
            Result.failure(e)
        }
    }

    private fun isVersionNewer(candidate: String, current: String): Boolean {
        if (candidate.isBlank()) return false
        val candidateParts = candidate.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(candidateParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val cand = candidateParts.getOrElse(i) { 0 }
            val curr = currentParts.getOrElse(i) { 0 }
            if (cand > curr) return true
            if (cand < curr) return false
        }
        return false
    }
}
