package com.userlab.movieplus.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Account returned by the UserLab server. */
data class UserLabUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String
)

/**
 * Outcome of a call to the UserLab auth server.
 * [httpCode] is 0 when the server could not be reached at all ([networkError] = true).
 */
data class UserLabAuthResult(
    val ok: Boolean,
    val message: String,
    val httpCode: Int = 0,
    val token: String? = null,
    val user: UserLabUser? = null,
    val networkError: Boolean = false
)

/**
 * Client for https://muziki.alwaysdata.net/api/auth.php (see /server in the project).
 * All registration and login data goes to this endpoint.
 */
object UserLabAuthApi {
    const val ENDPOINT = "https://muziki.alwaysdata.net/api/auth.php"

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun register(name: String, email: String, password: String, device: String): UserLabAuthResult =
        send(
            JSONObject()
                .put("action", "register")
                .put("name", name)
                .put("email", email)
                .put("password", password)
                .put("device", device),
            token = null
        )

    suspend fun login(email: String, password: String, device: String): UserLabAuthResult =
        send(
            JSONObject()
                .put("action", "login")
                .put("email", email)
                .put("password", password)
                .put("device", device),
            token = null
        )

    suspend fun me(token: String): UserLabAuthResult =
        send(JSONObject().put("action", "me"), token)

    suspend fun logout(token: String): UserLabAuthResult =
        send(JSONObject().put("action", "logout"), token)

    suspend fun updateProfile(token: String, name: String): UserLabAuthResult =
        send(JSONObject().put("action", "update_profile").put("name", name), token)

    suspend fun changePassword(token: String, current: String, new: String): UserLabAuthResult =
        send(
            JSONObject()
                .put("action", "change_password")
                .put("current_password", current)
                .put("new_password", new),
            token
        )

    suspend fun deleteAccount(token: String, password: String): UserLabAuthResult =
        send(JSONObject().put("action", "delete_account").put("password", password), token)

    suspend fun uploadAvatar(token: String, jpegOrPngBytes: ByteArray): UserLabAuthResult {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("action", "upload_avatar")
            .addFormDataPart(
                "avatar", "avatar.jpg",
                jpegOrPngBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()
        return execute(body, token)
    }

    // ── internals ──────────────────────────────────────────

    private suspend fun send(json: JSONObject, token: String?): UserLabAuthResult =
        execute(json.toString().toRequestBody(JSON), token)

    private suspend fun execute(body: RequestBody, token: String?): UserLabAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val builder = Request.Builder()
                    .url(ENDPOINT)
                    .header("Accept", "application/json")
                    .post(body)
                if (!token.isNullOrEmpty()) {
                    builder.header("Authorization", "Bearer $token")
                    builder.header("X-Auth-Token", token) // fallback if a proxy strips Authorization
                }
                client.newCall(builder.build()).execute().use { response ->
                    parse(response.code, response.body?.string().orEmpty())
                }
            } catch (e: java.io.IOException) {
                UserLabAuthResult(
                    ok = false,
                    message = "Can't reach UserLab servers. Check your internet connection and try again.",
                    networkError = true
                )
            } catch (e: Exception) {
                UserLabAuthResult(ok = false, message = "Unexpected error: ${e.message ?: "unknown"}")
            }
        }

    private fun parse(httpCode: Int, raw: String): UserLabAuthResult {
        val json = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return UserLabAuthResult(
                ok = false,
                message = "The server sent an unexpected response (HTTP $httpCode).",
                httpCode = httpCode
            )
        }
        val ok = httpCode in 200..299 && json.optString("status") == "success"
        val userJson = json.optJSONObject("user")
        val user = userJson?.let {
            UserLabUser(
                id = it.optString("id"),
                email = it.optString("email"),
                displayName = it.optString("display_name"),
                avatarUrl = it.optString("avatar_url")
            )
        }
        return UserLabAuthResult(
            ok = ok,
            message = json.optString("message").ifEmpty { if (ok) "OK" else "Request failed" },
            httpCode = httpCode,
            token = json.optString("token").takeIf { it.isNotEmpty() },
            user = user
        )
    }
}
