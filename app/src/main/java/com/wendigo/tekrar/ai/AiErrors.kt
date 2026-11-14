package com.wendigo.tekrar.ai

import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.MiniJsonException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AiClientException(
    message: String,
    val retryable: Boolean,
    val httpCode: Int? = null,
) : Exception(message)

data class AiErrorInfo(
    val messageTr: String,
    val retryable: Boolean,
)

object AiErrors {

    fun fromHttpCode(code: Int, serverMessage: String? = null): AiErrorInfo = when (code) {
        401 -> AiErrorInfo("Anahtar geçersiz. Ayarlardan anahtarı kontrol et.", false)
        403 -> AiErrorInfo("Bu anahtar bu modele erişemiyor.", false)
        404 -> AiErrorInfo(
            serverMessage?.let { "Model bulunamadı: $it" } ?: "Model bulunamadı. Model seçimini kontrol et.",
            false,
        )
        408 -> AiErrorInfo("Yanıt gecikti. Tekrar dene.", true)
        429 -> AiErrorInfo("Kota veya hız limiti doldu. Kısa bir süre sonra tekrar dene.", true)
        in 500..599 -> AiErrorInfo("Sunucu hatası. Kısa bir süre sonra tekrar dene.", true)
        in 400..499 -> {
            val detail = serverMessage?.takeIf { it.isNotBlank() }
            AiErrorInfo(
                detail?.let { "İstek reddedildi: $it" } ?: "İstek reddedildi.",
                false,
            )
        }
        else -> AiErrorInfo("Beklenmeyen sunucu yanıtı ($code).", false)
    }

    fun network(timeout: Boolean): AiErrorInfo = if (timeout) {
        AiErrorInfo("Yanıt gecikti. Bağlantını kontrol edip tekrar dene.", true)
    } else {
        AiErrorInfo("İnternet yok veya bağlantı kurulamadı.", true)
    }

    fun fromThrowable(error: Throwable): AiErrorInfo = when (error) {
        is SocketTimeoutException -> network(true)
        is UnknownHostException -> network(false)
        is ConnectException -> network(false)
        is InterruptedIOException -> network(true)
        is IOException -> network(false)
        else -> AiErrorInfo("Beklenmeyen bir hata oluştu.", false)
    }

    fun extractServerMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val root = try {
            MiniJson.parse(raw) as? Map<*, *>
        } catch (error: MiniJsonException) {
            null
        } ?: return null
        val message = when (val value = root["error"]) {
            is Map<*, *> -> value["message"] as? String
            is String -> value
            else -> null
        }
        return message
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(200)
            ?.takeIf { it.isNotEmpty() }
    }
}
