package com.wusatosi.recaptcha.internal

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Response
import com.google.gson.*
import com.wusatosi.recaptcha.UnexpectedError
import com.wusatosi.recaptcha.UnexpectedJsonStructure
import java.math.BigDecimal

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
    if (!this.expectPrimitive(attributeName).isBoolean)
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