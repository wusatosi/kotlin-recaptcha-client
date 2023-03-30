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
private const val HOSTNAME_ATTRIBUTE = "hostname"

internal abstract class RecaptchaClientBase(
    private val secretKey: String,
    recaptchaConfig: RecaptchaConfig
) : RecaptchaClient {

    private val client = HttpClient(recaptchaConfig.engine) {}
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
        val host: String
    )

    private fun errorCodeCheck(body: JsonObject): ErrorCode? {
        val errorCodes = body[ERROR_CODES_ATTRIBUTE]
            ?.let { it.expectStringArray(ERROR_CODES_ATTRIBUTE) }
            ?: listOf()

        if (errorCodes.isEmpty())
            return null

        if (INVALID_SITE_SECRET_KEY in errorCodes)
            throw InvalidSiteKeyException

        return errorCodes.firstNotNullOfOrNull {
            when (it) {
                INVALID_TOKEN_KEY -> ErrorCode.InvalidToken
                TIMEOUT_OR_DUPLICATE_KEY -> ErrorCode.TimeOrDuplicatedToken
                else -> null
            }
        } ?: throw UnexpectedError("Unexpected Error code: $errorCodes")
    }

    protected fun interpretResponseBody(body: JsonObject): Either<ErrorCode, BasicResponseBody> {
        val success = body[SUCCESS_ATTRIBUTE]
            .expectBoolean(SUCCESS_ATTRIBUTE)

        val errorCode = errorCodeCheck(body)
        if (errorCode != null)
            return Either.left(errorCode)

        val hostName = body[HOSTNAME_ATTRIBUTE]
            .expectString(HOSTNAME_ATTRIBUTE)
        val matchedHost = acceptableHosts.isEmpty() || (hostName.isNotEmpty() && hostName in acceptableHosts)
        return Either.right(BasicResponseBody(success, matchedHost, hostName))
    }

    override fun close() = client.close()

}
