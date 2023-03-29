package com.wusatosi.recaptcha.internal

import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaIOError
import com.wusatosi.recaptcha.UnexpectedError
import com.wusatosi.recaptcha.UnexpectedJsonStructure
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class RecaptchaClientBaseTest {

    private suspend fun simulateVerify(
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

    private fun simulateInterpretBody(body: String): Pair<Boolean, List<String>> {
        class Subject : RecaptchaClientBase("", false, MockEngine { respondOk() }) {
            override suspend fun verify(token: String): Boolean {
                return true
            }

            fun exposeInternal(payload: JsonObject): Pair<Boolean, List<String>> {
                return this.interpretResponseBody(payload)
            }
        }

        return Subject().exposeInternal(parseString(body).asJsonObject)
    }


    @Test
    fun properParameters() =
        runBlocking<Unit> {
            val siteKey = "key"
            val token = "token"

            val mockEngine = MockEngine {
                assertEquals("www.google.com", it.url.host)
                assertEquals("/recaptcha/api/siteverify", it.url.encodedPath)
                assertEquals("secret=$siteKey&response=$token", it.url.encodedQuery)
                assertEquals(HttpMethod.Post, it.method)
                respondOk("{}")
            }

            simulateVerify(mockEngine, siteKey, token)
        }

    @Test
    fun correctParsing() = runBlocking {
        val exampleReturn = """
            {"success":true,"challenge_ts":"2023-03-28T22:10:10Z","hostname":"wusatosi.com"}
        """.trimIndent()
        val mockEngine = MockEngine {
            respondOk(exampleReturn)
        }
        assertEquals(parseString(exampleReturn), simulateVerify(mockEngine))
    }

    @Test
    fun switchDomain() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                assertEquals(it.url.host, "www.recaptcha.net")
                respondOk("{}")
            }
            simulateVerify(mockEngine, useRecaptchaDotNetEndPoint = true)
        }

    @Test
    fun ioExceptionUponRequest() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                throw IOException("boom!")
            }
            assertThrows<RecaptchaIOError> { simulateVerify(mockEngine) }
        }

    @Test
    fun badStatus() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondBadRequest()
            }
            assertThrows<UnexpectedError> { simulateVerify(mockEngine) }
        }

    @Test
    fun badStatus2() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondError(HttpStatusCode.InternalServerError)
            }
            assertThrows<UnexpectedError> { simulateVerify(mockEngine) }
        }

    @Test
    fun malformedResponse() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondOk("{abcdefg")
            }
            assertThrows<UnexpectedJsonStructure> { simulateVerify(mockEngine) }
        }

    @Test
    fun malformedResponse2() =
        runBlocking<Unit> {
            val mockEngine = MockEngine {
                respondOk("abcdefg")
            }
            assertThrows<UnexpectedJsonStructure> { simulateVerify(mockEngine) }
        }

    @Test
    fun interpretSuccessBody() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
        val (success, errorCodes) = simulateInterpretBody(jsonStr)
        assert(success)
        assertEquals(listOf<String>(), errorCodes)
    }

    @Test
    fun interpretFailureBody() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
        val (success, errorCodes) = simulateInterpretBody(jsonStr)
        assert(!success)
        assertEquals(listOf<String>(), errorCodes)
    }

    @Test
    fun interpretFailureBody_EmptyErrorCode() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "error-codes": []
                }
            """.trimIndent()
            val (success, errorCodes) = simulateInterpretBody(jsonStr)
            assert(!success)
            assertEquals(listOf<String>(), errorCodes)
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "error-codes": []
                }
            """.trimIndent()
            val (success, errorCodes) = simulateInterpretBody(jsonStr)
            assert(success)
            assertEquals(listOf<String>(), errorCodes)
        }
    }


    @Test
    fun interpretFailureBody_malformed() =
        runBlocking {
            @Language("JSON") val missingAttribute = """
                {}
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(missingAttribute) }

            @Language("JSON") val typeMismatch = """
                {"success":  ":("}
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(typeMismatch) }

            @Language("JSON") val errorCodeMistype = """
                {
                  "success": "false",
                  "error-codes": [true]
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(errorCodeMistype) }

            Unit
        }

    @Test
    fun interpretFailureBody_invalidSiteSecret() =
        runBlocking {
            @Language("JSON") val singleError = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateInterpretBody(singleError) }

            @Language("JSON") val twoError = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-response", "invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateInterpretBody(twoError) }

            @Language("JSON") val threeError = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-response", "invalid-input-secret", "timeout-or-duplicate"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateInterpretBody(threeError) }

            Unit
        }

}
