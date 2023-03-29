package com.wusatosi.recaptcha.internal

import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.internal.RecaptchaClientBase.BasicResponseBody
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.net.URLEncoder

class RecaptchaClientBaseTest {

    private suspend fun simulateVerify(
        engine: HttpClientEngine,
        siteKey: String = "key",
        token: String = "token",
        useRecaptchaDotNetEndPoint: Boolean = false,
        remoteIp: String = ""
    ): JsonObject? {
        var result: JsonObject? = null

        val config = RecaptchaV2Config()
        config.useAlternativeDomain = useRecaptchaDotNetEndPoint
        config.engine = engine

        class Subject(
            siteKey: String,
        ) :
            RecaptchaClientBase(siteKey, config) {
            override suspend fun verify(token: String, remoteIp: String): Boolean {
                result = transact(token, remoteIp)
                return true
            }
        }

        Subject(siteKey).use { it.verify(token, remoteIp) }
        return result
    }

    private fun simulateInterpretBody(body: String): BasicResponseBody {
        val config = RecaptchaV2Config()
        config.engine = MockEngine { respondOk() }
        class Subject : RecaptchaClientBase("", config) {
            override suspend fun verify(token: String, remoteIp: String): Boolean {
                return true
            }

            fun exposeInternal(payload: JsonObject): BasicResponseBody {
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
    fun properParameters_withV4Ip() =
        runBlocking<Unit> {
            val siteKey = "key"
            val token = "token"
            val remoteIpV4 = "1.2.3.4"

            val mockEngine = MockEngine {
                assertEquals("www.google.com", it.url.host)
                assertEquals("/recaptcha/api/siteverify", it.url.encodedPath)
                assertEquals("secret=$siteKey&response=$token&remoteip=$remoteIpV4", it.url.encodedQuery)
                assertEquals(HttpMethod.Post, it.method)
                respondOk("{}")
            }

            simulateVerify(mockEngine, siteKey, token, remoteIp = remoteIpV4)
        }

    @Test
    fun properParameters_withV6Ip() =
        runBlocking<Unit> {
            val siteKey = "key"
            val token = "token"
            val remoteIpV6 = "1111:2222:3333:4444:5555:6666:7777:8888"

            val mockEngine = MockEngine {
                assertEquals("www.google.com", it.url.host)
                assertEquals("/recaptcha/api/siteverify", it.url.encodedPath)
                assertEquals(
                    "secret=$siteKey&response=$token&remoteip=${URLEncoder.encode(remoteIpV6, "UTF-8")}",
                    it.url.encodedQuery
                )
                assertEquals(HttpMethod.Post, it.method)
                respondOk("{}")
            }

            simulateVerify(mockEngine, siteKey, token, remoteIp = remoteIpV6)
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
        val (success, _, errorCodes) = simulateInterpretBody(jsonStr)
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
        val (success, _, errorCodes) = simulateInterpretBody(jsonStr)
        assert(!success)
        assertEquals(listOf<String>(), errorCodes)
    }

    @Test
    fun interpretFailureBody_EmptyErrorCode() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "hostname": "wusatosi.com",
                  "error-codes": []
                }
            """.trimIndent()
            val (success, _, errorCodes) = simulateInterpretBody(jsonStr)
            assert(!success)
            assertEquals(listOf<String>(), errorCodes)
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "error-codes": []
                }
            """.trimIndent()
            val (success, _, errorCodes) = simulateInterpretBody(jsonStr)
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
                  "hostname": "wusatosi.com",
                  "error-codes": ["invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateInterpretBody(singleError) }

            @Language("JSON") val twoError = """
                {
                  "success": false,
                  "hostname": "wusatosi.com",
                  "error-codes": ["invalid-input-response", "invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateInterpretBody(twoError) }

            @Language("JSON") val threeError = """
                {
                  "success": false,
                  "hostname": "wusatosi.com",
                  "error-codes": ["invalid-input-response", "invalid-input-secret", "timeout-or-duplicate"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateInterpretBody(threeError) }

            Unit
        }

}
