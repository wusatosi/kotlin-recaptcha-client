package com.wusatosi.recaptcha.internal

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.result.Result
import com.google.gson.*
import com.wusatosi.recaptcha.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.regex.Pattern

internal const val issue_address = "https://github.com/wusatosi/kotlin-recaptcha-client/issues/new"

internal suspend fun getJsonObj(baseURL: String, token: String): JsonObject {
//    Fuel 1.16.0 will not do request within IO dispatcher, it blocks the process... (fixed in current master branch)
    val (_, _, result) = withContext(Dispatchers.IO) {
        Fuel
            .post(baseURL + token)
            .awaitResponse(JsonResponseDeserializer)
    }

    if (result is Result.Failure) {
        val cause = result.error.cause
        when (cause) {
            is JsonParseException -> throw UnableToDeserializeError(cause, result.error.response)
            is IOException -> throw RecaptchaIOError(cause)
            else ->
                throw UnexpectedError(
                    "Unexpected error occurred when requesting verification ${cause ?: ""}",
                    cause
                )
        }
    }

    val res = result.get()
    if (!res.isJsonObject)
        throw UnexpectedJsonStructure("response json isn't an object")

    return res.asJsonObject
}

private val pattern = Pattern.compile("^[-a-zA-Z0-9+&@#/%?=~_!:,.;]*[-a-zA-Z0-9+&@#/%=~_]")
internal fun checkURLCompatibility(target: String): Boolean = pattern.matcher(target).matches()

internal fun checkErrorCodes(
    errorCodes: JsonArray,
    invalidate_token_score: Double = 0.0,
    timeout_or_duplicate_score: Double = 0.0
): Double {
    for (errorCode in errorCodes) {
        val string = errorCode.expectString("error-codes")
//                Error code	            Description
//                missing-input-secret	    The secret parameter is missing.
//                invalid-input-secret	    The secret parameter is invalid or malformed.
//                missing-input-response	The response parameter is missing.
//                invalid-input-response	The response parameter is invalid or malformed.
//                bad-request	            The request is invalid or malformed.
//                timeout-or-duplicate      Timeout... (didn't include in the v3 documentation)
        when {
            string.startsWith("missing-") || string == "bad-request" ->
                throw UnexpectedError("assertion failed, should not report $string", null)

            string == "invalid-input-secret" ->
                throw InvalidSiteKeyException

            string == "invalid-input-response" ->
                return invalidate_token_score

            string == "timeout-or-duplicate" ->
                return timeout_or_duplicate_score

            else ->
                UnexpectedJsonStructure("unexpected error code: $string")
        }
    }
    throw UnexpectedJsonStructure("empty error code")
}

internal fun JsonElement?.expectArray(attributeName: String): JsonArray {
    this.expectNonNull(attributeName)
    if (!this!!.isJsonArray)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an array"
        )
    return this.asJsonArray
}

internal fun JsonElement?.expectPrimitive(attributeName: String): JsonPrimitive {
    this.expectNonNull(attributeName)
    if (!this!!.isJsonPrimitive)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not a boolean"
        )
    return this.asJsonPrimitive
}

internal fun JsonElement?.expectObject(attributeName: String): JsonObject {
    this.expectNonNull(attributeName)
    if (!this!!.isJsonObject)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an object"
        )
    return this.asJsonObject
}

internal fun JsonElement?.expectString(attributeName: String): String {
    this.expectNonNull(attributeName)
    if (!this.expectPrimitive(attributeName).isString)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an string"
        )
    return this!!.asString
}

internal fun JsonPrimitive.expectBoolean(attributeName: String): Boolean {
    if (!this.isBoolean)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not a boolean"
        )
    return this.asBoolean
}

internal fun JsonPrimitive.expectNumber(attributeName: String): JsonPrimitive {
    if (!this.isNumber)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an number"
        )
    return this
}

private fun JsonElement?.expectNonNull(attributeName: String) {
    this ?: throw UnexpectedJsonStructure(
        "$attributeName do not exists"
    )
}

private object JsonResponseDeserializer : Deserializable<JsonElement> {
    override fun deserialize(response: Response): JsonElement {
        val statusCode = response.statusCode
        if (statusCode !in 200..299)
            throw UnexpectedError(
                "Google is down (seems like), server returns status code: $statusCode, " +
                        "body: ${kotlin.runCatching { String(response.data) }.getOrElse { "unavailable" }}",
                null
            )
        return JsonParser().parse(String(response.data))
    }
}
