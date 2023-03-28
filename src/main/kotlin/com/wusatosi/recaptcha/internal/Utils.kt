package com.wusatosi.recaptcha.internal

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.UnexpectedError
import com.wusatosi.recaptcha.UnexpectedJsonStructure
import java.util.regex.Pattern

internal const val issue_address = "https://github.com/wusatosi/kotlin-recaptcha-client/issues/new"

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

internal fun JsonElement.expectBoolean(attributeName: String) = this
    .expectPrimitive(attributeName)
    .expectBoolean(attributeName)

internal fun JsonPrimitive.expectNumber(attributeName: String): JsonPrimitive {
    if (!this.isNumber)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an number"
        )
    return this
}

internal fun JsonElement.expectNumber(attributeName: String) = this
    .expectPrimitive(attributeName)
    .expectNumber(attributeName)

private fun JsonElement?.expectNonNull(attributeName: String) {
    this ?: throw UnexpectedJsonStructure(
        "$attributeName do not exists"
    )
}
