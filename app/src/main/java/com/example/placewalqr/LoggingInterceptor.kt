package com.example.placewalqr

import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log
import okio.Buffer

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Log della request
        Log.d("HTTP-REQUEST", "➡ ${request.method} ${request.url}")
        request.headers.forEach {
            Log.d("HTTP-REQUEST", "${it.first}: ${it.second}")
        }

        // Corpo della request (se presente)
        request.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                Log.d("HTTP-REQUEST-BODY", buffer.readUtf8())
            } catch (e: Exception) {
                Log.e("HTTP-REQUEST-BODY", "Errore lettura body: ${e.message}")
            }
        }

        val response = chain.proceed(request)

        // Log della response
        Log.d("HTTP-RESPONSE", "⬅ Code: ${response.code} | Message: ${response.message}")

        return response
    }
}
