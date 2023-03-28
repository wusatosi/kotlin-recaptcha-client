package com.wusatosi.recaptcha.internal

import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import com.wusatosi.recaptcha.RecaptchaIOError
import com.wusatosi.recaptcha.UnableToDeserializeError
import com.wusatosi.recaptcha.UnexpectedError
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class RecaptchaClientBaseTest {

    private suspend fun simulate(
        engine: HttpClientEngine,
        siteKey: String = "key",
        token: String = "token",
        useRecaptchaDotNetEndPoint: Boolean = false
    ): JsonObject? {
        var result: JsonObject? = null

        class Subject(
            engine: HttpClientEngine,
            siteKey: String,
            useRecaptchaDotNetEndPoint: Boolean,
        ) :
            RecaptchaClientBase(siteKey, useRecaptchaDotNetEndPoint, engine) {
            override suspend fun verify(token: String): Boolean {
                result = transact(token)
                return true
            }
        }

        Subject(engine, siteKey, useRecaptchaDotNetEndPoint).use { it.verify(token) }
        return result
    }


    @Test
    fun properParameters() =
        runBlocking<Unit> {
            val siteKey = "key"
            val token = "token"

            val mockEngine = MockEngine {
                assertEquals(it.url.host, "www.google.com")
                assertEquals(it.url.encodedPath, "/recaptcha/api/siteverify")
                assertEquals(it.url.encodedQuery, "secret=$siteKey&response=$token")
                assertEquals(it.method, HttpMethod.Post)
                respondOk("{}")
            }

            simulate(mockEngine, siteKey, token)
        }

    @Test
    fun correctParsing() = runBlocking {
        val exampleReturn = """
            {"success":true,"challenge_ts":"2023-03-28T22:10:10Z","hostname":"wusatosi.com"}
        """.trimIndent()
        val mockEngine = MockEngine {
            respondOk(exampleReturn)
        }
        assertEquals(parseString(exampleReturn), simulate(mockEngine))
    }

    @Test
    fun switchDomain() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                assertEquals(it.url.host, "www.recaptcha.net")
                respondOk("{}")
            }
            simulate(mockEngine, useRecaptchaDotNetEndPoint = true)
        }

    @Test
    fun ioExceptionUponRequest() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                throw IOException("boom!")
            }
            assertThrows<RecaptchaIOError> { simulate(mockEngine) }
        }

    @Test
    fun badStatus() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondBadRequest()
            }
            assertThrows<UnexpectedError> { simulate(mockEngine) }
        }

    @Test
    fun badStatus2() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondError(HttpStatusCode.InternalServerError)
            }
            assertThrows<UnexpectedError> { simulate(mockEngine) }
        }

    @Test
    fun malformedResponse() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondOk("{abcdefg")
            }
            assertThrows<UnableToDeserializeError> { simulate(mockEngine) }
        }

    @Test
    fun malformedResponse2() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondOk("abcdefg")
            }
            assertThrows<UnableToDeserializeError> { simulate(mockEngine) }
        }

}
