package com.example.intellipatassignment.data.remote

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import com.example.intellipatassignment.core.ConnectivityObserver
import com.example.intellipatassignment.data.session.SessionStorage
import com.example.intellipatassignment.domain.error.NoConnectivityException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import java.io.IOException

class DebugLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()
        return try {
            chain.proceed(request).also {
                val ms = (System.nanoTime() - startNs) / NANOS_PER_MILLI
                Log.d(TAG, "${request.method} ${request.url.encodedPath} -> ${it.code} (${ms}ms)")
            }
        } catch (e: IOException) {
            Log.d(TAG, "${request.method} ${request.url.encodedPath} failed: ${e::class.java.simpleName}")
            throw e
        }
    }

    private companion object {
        const val TAG = "Http"
        const val NANOS_PER_MILLI = 1_000_000
    }
}

class AuthInterceptor(private val session: SessionStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.getToken() ?: return chain.proceed(chain.request())
        return chain.proceed(
            chain.request().newBuilder().header("Authorization", "Bearer $token").build(),
        )
    }
}

class ConnectivityInterceptor(
    private val connectivity: ConnectivityObserver,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!connectivity.isCurrentlyOnline()) throw NoConnectivityException()
        return chain.proceed(chain.request())
    }
}

object MockServerPrefs {
    const val NAME = "mock_server"
    const val KEY_COMPLETED_IDS = "completed_lesson_ids"
}

class MockApiInterceptor(
    private val assets: AssetManager,
    private val prefs: SharedPreferences,
    private val latencyMs: Long = DEFAULT_LATENCY_MS,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(latencyMs)
        val path = request.url.encodedPath
        val (code, body) = when {
            request.header("Authorization") == null -> HTTP_UNAUTHORIZED to error("unauthorized")
            request.method == "GET" && path.endsWith("/courses") -> HTTP_OK to coursesJson()
            request.method == "POST" && path.endsWith("/complete") -> {
                val lessonId = path.removeSuffix("/complete").substringAfterLast('/')
                prefs.edit().putStringSet(MockServerPrefs.KEY_COMPLETED_IDS, completedIds() + lessonId).apply()
                HTTP_OK to "{}"
            }
            else -> HTTP_NOT_FOUND to error("not found")
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == HTTP_OK) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun error(message: String) = """{"error":"$message"}"""

    private fun completedIds(): Set<String> =
        prefs.getStringSet(MockServerPrefs.KEY_COMPLETED_IDS, emptySet()).orEmpty()

    private fun coursesJson(): String {
        val done = completedIds()
        val courses = JSONArray(assets.open("courses.json").bufferedReader().use { it.readText() })
        for (c in 0 until courses.length()) {
            val lessons = courses.getJSONObject(c).getJSONArray("lessonItems")
            for (l in 0 until lessons.length()) {
                val lesson = lessons.getJSONObject(l)
                if (lesson.getInt("id").toString() in done) lesson.put("completed", true)
            }
        }
        return courses.toString()
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 800L
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
    }
}
