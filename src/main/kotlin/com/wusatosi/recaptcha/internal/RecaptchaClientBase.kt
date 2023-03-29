package com.wusatosi.recaptcha.internal

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.wusatosi.recaptcha.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException

private const val DEFAULT_DOMAIN = "www.google.com"
private const val ALTERNATE_DOMAIN = "www.recaptcha.net"
private const val VALIDATION_PATH = "/recaptcha/api/siteverify"

//  Error code	             Description
//  missing-input-secret	 The secret parameter is missing.
//  invalid-input-secret	 The secret parameter is invalid or malformed.
//  missing-input-response	 The response parameter is missing.
//  invalid-input-response	 The response parameter is invalid or malformed.
//  bad-request	             The request is invalid or malformed.
//  timeout-or-duplicate     Timeout... (didn't include in the v3 documentation)
internal const val INVALID_SITE_SECRET_KEY = "invalid-input-secret"
internal const val INVALID_TOKEN_KEY = "invalid-input-response"
internal const val TIMEOUT_OR_DUPLICATE_KEY = "timeout-or-duplicate"

private const val SUCCESS_ATTRIBUTE = "success"
private const val ERROR_CODES_ATTRIBUTE = "error-codes"

internal abstract class RecaptchaClientBase(
    private val secretKey: String,
    recaptchaConfig: RecaptchaConfig
) : RecaptchaClient {

    private val client: HttpClient = HttpClient(recaptchaConfig.engine) {}
    private val validateHost = if (!recaptchaConfig.useAlternativeDomain) DEFAULT_DOMAIN else ALTERNATE_DOMAIN
    private val acceptableHosts: List<String> = ArrayList(recaptchaConfig.hostList)

    protected suspend fun transact(token: String, remoteIp: String): JsonObject {
        val response = executeRequest(token, remoteIp)
        checkResponseStatus(response)
        try {
            val body = JsonParser.parseString(response.bodyAsText())
            if (!body.isJsonObject)
                throwUnexpectedJsonStructure()
            return body.asJsonObject
        } catch (error: JsonParseException) {
            throwUnexpectedJsonStructure(error)
        }
    }

    private fun throwUnexpectedJsonStructure(error: JsonParseException? = null): Nothing =
        throw UnexpectedJsonStructure("The server did not respond with a valid Json object", error)

    private fun checkResponseStatus(response: HttpResponse) {
        val status = response.status
        if (status.value !in 200..299)
            throw UnexpectedError("Invalid respond status code: ${status.value}, ${status.description}", null)
    }

    private suspend fun executeRequest(token: String, remoteIp: String) = try {
        client.post {
            url {
                protocol = URLProtocol.HTTPS
                host = validateHost
                path(VALIDATION_PATH)
                parameters.append("secret", secretKey)
                parameters.append("response", token)
                if (remoteIp.isNotEmpty())
                    parameters.append("remoteip", remoteIp)
            }
        }
    } catch (io: IOException) {
        throw RecaptchaIOError(io)
    }

    internal data class BasicResponseBody(
        val success: Boolean,
        val matchedHost: Boolean,
        val errorCodes: List<String>,
        val host: String
    )

    protected fun interpretResponseBody(body: JsonObject): BasicResponseBody {
        val success = body[SUCCESS_ATTRIBUTE]
            .expectBoolean(SUCCESS_ATTRIBUTE)
        val errorCodes = body[ERROR_CODES_ATTRIBUTE]
            ?.let { it.expectStringArray(ERROR_CODES_ATTRIBUTE) }
            ?: listOf()

        if (INVALID_SITE_SECRET_KEY in errorCodes)
            throw InvalidSiteKeyException

        val hostName = body["hostname"]
            .expectString("hostname")
        val matchedHost = acceptableHosts.isEmpty() || hostName in acceptableHosts

        return BasicResponseBody(success, matchedHost, errorCodes, hostName)
    }

    override fun close() = client.close()

}
