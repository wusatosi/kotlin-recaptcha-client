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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.net.URLEncoder

class RecaptchaClientBaseTest {

    private suspend fun simulateTransact(
        engine: HttpClientEngine,
        siteKey: String = "key",
        token: String = "token",
        useAlternativeDomain: Boolean = false,
        remoteIp: String = "",
    ): JsonObject? {
        var result: JsonObject? = null

        val config = BaseConfig()
        config.useAlternativeDomain = useAlternativeDomain
        config.engine = engine

        class Subject(
            siteKey: String,
        ) : RecaptchaClientBase(siteKey, config) {
            override suspend fun verify(token: String, remoteIp: String): Boolean {
                result = transact(token, remoteIp)
                return true
            }
        }

        Subject(siteKey).use { it.verify(token, remoteIp) }
        return result
    }

    private fun simulateInterpretBody(
        body: String,
        block: BaseConfig.() -> Unit = {}
    ): Either<ErrorCode, BasicResponseBody> {
        val config = BaseConfig()
        config.engine = MockEngine { respondOk() }
        block(config)
        class Subject : RecaptchaClientBase("", config) {
            override suspend fun verify(token: String, remoteIp: String): Boolean {
                return true
            }

            fun exposeInternal(payload: JsonObject): Either<ErrorCode, BasicResponseBody> {
                return this.interpretResponseBody(payload)
            }
        }

        return Subject().exposeInternal(parseString(body).asJsonObject)
    }


    @Test
    fun properParameters() = runBlocking {
        val siteKey = "key"
        val token = "token"

        val mockEngine = MockEngine {
            assertEquals("www.google.com", it.url.host)
            assertEquals("/recaptcha/api/siteverify", it.url.encodedPath)
            assertEquals("secret=$siteKey&response=$token", it.url.encodedQuery)
            assertEquals(HttpMethod.Post, it.method)
            respondOk("{}")
        }

        simulateTransact(mockEngine, siteKey, token)

        Unit
    }

    @Test
    fun properParameters_withV4Ip() = runBlocking {
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

        simulateTransact(mockEngine, siteKey, token, remoteIp = remoteIpV4)

        Unit
    }

    @Test
    fun properParameters_withV6Ip() = runBlocking {
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

        simulateTransact(mockEngine, siteKey, token, remoteIp = remoteIpV6)

        Unit
    }

    @Test
    fun correctParsing() = runBlocking {
        val exampleReturn = """
            {"success":true,"challenge_ts":"2023-03-28T22:10:10Z","hostname":"wusatosi.com"}
        """.trimIndent()
        val mockEngine = MockEngine {
            respondOk(exampleReturn)
        }
        assertEquals(parseString(exampleReturn), simulateTransact(mockEngine))
    }

    @Test
    fun switchDomain() = runBlocking {
        val mockEngine = MockEngine {
            assertEquals(it.url.host, "www.recaptcha.net")
            respondOk("{}")
        }
        simulateTransact(mockEngine, useAlternativeDomain = true)

        Unit
    }

    @Test
    fun ioExceptionUponRequest() = runBlocking {
        val mockEngine = MockEngine {
            throw IOException("boom!")
        }
        assertThrows<RecaptchaIOError> { simulateTransact(mockEngine) }

        Unit
    }

    @Test
    fun badStatus() = runBlocking {
        val mockEngine = MockEngine {
            respondBadRequest()
        }
        assertThrows<UnexpectedError> { simulateTransact(mockEngine) }

        Unit
    }

    @Test
    fun badStatus2() = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.InternalServerError)
        }
        assertThrows<UnexpectedError> { simulateTransact(mockEngine) }

        Unit
    }

    @Test
    fun malformedResponse() = runBlocking {
        val mockEngine = MockEngine {
            respondOk("{abcdefg")
        }
        assertThrows<UnexpectedJsonStructure> { simulateTransact(mockEngine) }

        Unit
    }

    @Test
    fun malformedResponse2() = runBlocking {
        val mockEngine = MockEngine {
            respondOk("abcdefg")
        }
        assertThrows<UnexpectedJsonStructure> { simulateTransact(mockEngine) }

        Unit
    }

    @Test
    fun interpretSuccessBody() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
        val either = simulateInterpretBody(jsonStr)
        assertTrue(either is Right)
        val (success, _, _) = either.right
        assertTrue(success)
    }

    @Test
    fun interpretSuccessBody_matchingHost() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomain("wusatosi.com")
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomain("wusatosi.com", "google.com")
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "hostname": "google.com"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomain("wusatosi.com", "google.com")
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomains(listOf("wusatosi.com", "google.com"))
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "hostname": "google.com"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomains(listOf("wusatosi.com", "google.com"))
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
    }

    @Test
    fun interpretSuccessBody_apkName() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "apk_package_name": "com.wusatosi.test"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomain("com.wusatosi.test")
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "apk_package_name": "com.wusatosi.test"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomain("com.wusatosi.test", "com.google.map")
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "apk_package_name": "com.google.map"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomain("com.wusatosi.test", "com.google.map")
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "apk_package_name": "com.wusatosi.test"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomains(listOf("com.wusatosi.test", "com.google.map"))
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "apk_package_name": "com.google.map"
                    }
                """.trimIndent()
            val either = simulateInterpretBody(jsonStr) {
                allowDomains(listOf("com.wusatosi.test", "com.google.map"))
            }
            assertTrue(either is Right)
            val (_, domainMatch, _) = either.right
            assertTrue(domainMatch)
        }
    }

    @Test
    fun interpretSuccessBody_mismatchDomain() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
        val either = simulateInterpretBody(jsonStr) {
            allowDomain("google.com")
        }
        assertTrue(either is Right)
        val (_, domainMatch, _) = either.right
        assertFalse(domainMatch)
    }

    @Test
    fun interpretSuccessBody_mismatchApk() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "apk_package_name": "com.wusatosi.test"
                }
            """.trimIndent()
        val either = simulateInterpretBody(jsonStr) {
            allowDomain("com.google.map")
        }
        assertTrue(either is Right)
        val (_, domainMatch, _) = either.right
        assertFalse(domainMatch)
    }

    @Test
    fun interpretFailureBody() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
        val either = simulateInterpretBody(jsonStr)
        assertTrue(either is Right)
        val (success, _, _) = either.right
        assertFalse(success)
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
            val either = simulateInterpretBody(jsonStr)
            assertTrue(either is Right)
            val (success, _, _) = either.right
            assertFalse(success)
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "error-codes": []
                }
            """.trimIndent()
            val either = simulateInterpretBody(jsonStr)
            assertTrue(either is Right)
            val (success, _, _) = either.right
            assertTrue(success)
        }
    }


    @Test
    fun interpretFailureBody_malformed() = runBlocking {
        @Language("JSON") val missingAttribute = """
                {}
            """.trimIndent()
        assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(missingAttribute) }

        @Language("JSON") val typeMismatch = """
                {"success":  ":("}
            """.trimIndent()
        assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(typeMismatch) }

        @Language("JSON") val errorCodeMisSubtype = """
                {
                  "success": false,
                  "error-codes": [true]
                }
            """.trimIndent()
        assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(errorCodeMisSubtype) }

        @Language("JSON") val errorCodeMistype = """
                {
                  "success": false,
                  "error-codes": true
                }
            """.trimIndent()
        assertThrows<UnexpectedJsonStructure> { simulateInterpretBody(errorCodeMistype) }

        Unit
    }

    @Test
    fun interpretFailureBody_invalidSiteSecret() = runBlocking {
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

    @Test
    fun interpretErrorCodes_InvalidToken() = runBlocking {
        @Language("JSON") val invalidToken = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-response"]
                }
            """.trimIndent()
        val either = simulateInterpretBody(invalidToken)
        assertTrue(either is Left)
        val errorCode = either.left
        assertEquals(ErrorCode.InvalidToken, errorCode)
    }

    @Test
    fun interpretErrorCodes_Timeout() = runBlocking {
        @Language("JSON") val invalidToken = """
                {
                  "success": false,
                  "error-codes": ["timeout-or-duplicate"]
                }
            """.trimIndent()
        val either = simulateInterpretBody(invalidToken)
        assertTrue(either is Left)
        val errorCode = either.left
        assertEquals(ErrorCode.TimeOrDuplicatedToken, errorCode)
    }

    @Test
    fun interpretErrorCodes_InvalidErrorCode() = runBlocking {
        @Language("JSON") val invalidToken = """
                {
                  "success": false,
                  "error-codes": ["boom"]
                }
            """.trimIndent()
        assertThrows<UnexpectedError> { simulateInterpretBody(invalidToken) }

        Unit
    }

}
