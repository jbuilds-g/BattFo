package com.example.data.github

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubProfile(
    val login: String,
    val name: String?,
    val avatar: Bitmap?
)

object GitHubProfileRepository {
    private const val username = "jbuilds-g"
    private const val apiUrl = "https://api.github.com/users/$username"

    suspend fun loadProfile(): GitHubProfile? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "BattFo")
            }

            val profileJson = connection.inputStream.use { input ->
                input.bufferedReader().readText()
            }
            connection.disconnect()

            val json = JSONObject(profileJson)
            val avatarUrl = json.optString("avatar_url").takeIf { it.isNotBlank() }

            GitHubProfile(
                login = json.optString("login", username),
                name = json.optString("name").takeIf { it.isNotBlank() },
                avatar = avatarUrl?.let(::downloadAvatar)
            )
        }.getOrNull()
    }

    private fun downloadAvatar(url: String): Bitmap? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("User-Agent", "BattFo")
        }

        connection.inputStream.use { input ->
            BitmapFactory.decodeStream(input)
        }.also {
            connection.disconnect()
        }
    }.getOrNull()
}
