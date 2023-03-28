package com.wusatosi.recaptcha.internal

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.wusatosi.recaptcha.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException

internal abstract class RecaptchaClientBase(
    private val secretKey: String,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClient {

    protected val client: HttpClient = HttpClient(engine) {}
    private val validateHost = if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com"
    private val path = "/recaptcha/api/siteverify"

    protected suspend fun transact(token: String): JsonObject {
        val response =
            try {
                client.post {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = validateHost
                        path(path)
                        parameters.append("secret", secretKey)
                        parameters.append("response", token)
                    }
                }
            } catch (io: IOException) {
                throw RecaptchaIOError(io)
            }

        val status = response.status
        if (status.value !in 200..299)
            throw UnexpectedError("Invalid respond status code: ${status.value}, ${status.description}", null)

        return try {
            val body = JsonParser.parseString(response.bodyAsText())
            body.asJsonObject!!
        } catch (error: JsonParseException) {
            throw UnexpectedJsonStructure("The server did not respond with a valid Json object", error)
        }
    }

    override fun close() = client.close()

}