package com.wusatosi.recaptcha.internal

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.result.Result
import com.google.gson.*
import com.wusatosi.recaptcha.*
import java.io.IOException
import java.util.regex.Pattern

/**
 * get the json response object from recaptcha remote, if your site key is correct, expect only IOException
 *
 * @param token the client's response token
 * @throws IOException self explanatory
 * @throws RecaptchaError if the server responded an invalid json structure
 * @throws InvalidSiteKeyException if server responded with error code: invalid-input-secret
 */
@Throws(IOException::class)
internal suspend fun getJsonObj(baseURL: String, token: String): JsonObject {
    val (_, _, result) = Fuel
        .post(baseURL + token)
        .awaitResponse(JsonResponseDeserializer)
    if (result is Result.Failure) {
        val cause = result.error.cause
        when (cause) {
            is JsonParseException -> throw UnableToDeserializeError(cause, result.error.response)
            is IOException -> throw cause
            else ->
                throw UnexpectedError(
                    "Unexpected error occurred when requesting verification ${cause ?: ""}",
                    cause
                )
        }
    }

    val res = result.get()
    if (!res.isJsonObject)
        throw UnexpectedJsonStructure("respond json isn't an object")

    return res.asJsonObject
}

private val pattern = Pattern.compile("^[-a-zA-Z0-9+&@#/%?=~_!:,.;]*[-a-zA-Z0-9+&@#/%=~_]")

internal fun checkURLCompatibility(target: String): Boolean = pattern.matcher(target).matches()

internal const val INVALID_INPUT_RESPONSE: Int = 0
internal const val TIMEOUT_OR_DUPLICATE: Int = 1

internal fun processErrorCodes(errorCodes: JsonArray): Int {
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
                throw InvalidSiteKeyException()

            string == "invalid-input-response" ->
                return INVALID_INPUT_RESPONSE

            string == "timeout-or-duplicate" ->
                return TIMEOUT_OR_DUPLICATE

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

internal object JsonResponseDeserializer : Deserializable<JsonElement> {
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
