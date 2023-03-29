package com.wusatosi.recaptcha.internal

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.UnexpectedError
import com.wusatosi.recaptcha.UnexpectedJsonStructure
import java.util.regex.Pattern

private val pattern = Pattern.compile("^[-a-zA-Z0-9+&@#/%?=~_!:,.;]*[-a-zA-Z0-9+&@#/%=~_]")
internal fun checkURLCompatibility(target: String): Boolean = pattern.matcher(target).matches()

//  Error code	             Description
//  missing-input-secret	 The secret parameter is missing.                        [x]
//  invalid-input-secret	 The secret parameter is invalid or malformed.           [1]
//  missing-input-response	 The response parameter is missing.                      [x]
//  invalid-input-response	 The response parameter is invalid or malformed.         [2]
//  bad-request	             The request is invalid or malformed.                    [x]
//  timeout-or-duplicate     Timeout... (didn't include in the v3 documentation)     [3]

//  By severity, Invalid site secret > Invalid token (input response) > Timeout or duplicate.
//  there's something wrong with this client.
//  We don't need to check missing-xxx, or bad-request, if we get those error codes,

private const val INVALID_SITE_SECRET = "invalid-input-secret"

internal fun checkSiteSecretError(errorCodes: List<String>) {
    if (INVALID_SITE_SECRET in errorCodes)
        throw InvalidSiteKeyException
}

private const val INVALID_TOKEN = "invalid-input-response"
private const val TIMEOUT_OR_DUPLICATE = "timeout-or-duplicate"

internal fun mapErrorCodes(
    errorCodes: List<String>,
    invalidTokenScore: Double,
    timeoutOrDuplicateTokenScore: Double
): Double {
    checkSiteSecretError(errorCodes)
    if (INVALID_TOKEN in errorCodes)
        return invalidTokenScore
    if (TIMEOUT_OR_DUPLICATE in errorCodes)
        return timeoutOrDuplicateTokenScore
    throw UnexpectedError("unexpected error codes: $errorCodes")
}

internal fun JsonElement?.expectStringArray(attributeName: String): List<String> {
    this ?: throwNull(attributeName)
    if (!this.isJsonArray)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an array"
        )
    return this.asJsonArray.map { it.expectString(attributeName) }
}

internal fun JsonElement?.expectPrimitive(attributeName: String): JsonPrimitive {
    this ?: throwNull(attributeName)
    if (!this.isJsonPrimitive)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not a boolean"
        )
    return this.asJsonPrimitive
}

internal fun JsonElement?.expectString(attributeName: String): String {
    this ?: throwNull(attributeName)
    if (!this.expectPrimitive(attributeName).isString)
        throw UnexpectedJsonStructure(
            "$attributeName attribute is not an string"
        )
    return this.asString
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

private fun throwNull(attributeName: String): Nothing {
    throw UnexpectedJsonStructure(
        "$attributeName do not exists"
    )
}
