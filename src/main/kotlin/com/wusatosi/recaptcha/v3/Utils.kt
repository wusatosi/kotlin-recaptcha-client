package com.wusatosi.recaptcha.v3

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Response
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.wusatosi.recaptcha.UnexpectedError

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
