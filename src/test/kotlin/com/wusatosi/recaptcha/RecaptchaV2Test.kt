package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import com.wusatosi.recaptcha.v2.RecaptchaV2Client.V2Decision
import com.wusatosi.recaptcha.v2.RecaptchaV2Client.V2ResponseDetail
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RecaptchaV2Test {

    private suspend fun simulateVerify(
        jsonStr: String,
        token: String = "token",
        block: RecaptchaV2Config.() -> Unit = {}
    ): Boolean {
        val engine = MockEngine {
            respondOk(jsonStr)
        }
        val client = RecaptchaV2Client.create("site") {
            block(this)
            this.engine = engine
        }
        return client.use { it.verify(token) }
    }

    private suspend fun simulateDetails(
        jsonStr: String,
        token: String = "token",
        block: RecaptchaV2Config.() -> Unit = {}
    ): Either<ErrorCode, Pair<V2ResponseDetail, V2Decision>> {
        val engine = MockEngine {
            respondOk(jsonStr)
        }
        val client = RecaptchaV2Client.create("site") {
            block(this)
            this.engine = engine
        }
        return client.use { it.getDetailedResponse(token) }
    }

    @Test
    fun testCreation() = runBlocking {
        RecaptchaV2Client.create("site")
        RecaptchaV2Client.create("site") {}

        Unit
    }

    @Test
    fun shouldNotInitialize_siteKeyPreCheck() = runBlocking {
        assertThrows<InvalidSiteKeyException> { RecaptchaV2Client.create("阿") }
        assertThrows<InvalidSiteKeyException> { RecaptchaV2Client.create("") }

        Unit
    }

    @Test
    fun success() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
            val expectedDetail = Either.right<ErrorCode, _>(
                V2ResponseDetail(true, "wusatosi.com") to V2Decision(
                    decision = true,
                    domainMatch = true
                )
            )
            assertTrue(simulateVerify(jsonStr))
            assertTrue(simulateVerify(jsonStr) { useAlternativeDomain = true })
            assertEquals(expectedDetail, simulateDetails(jsonStr))
            assertEquals(expectedDetail, simulateDetails(jsonStr) { useAlternativeDomain = true })
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "github.com"
                    }
                """.trimIndent()
            val expectedDetail = Either.right<ErrorCode, _>(
                V2ResponseDetail(true, "github.com") to V2Decision(
                    decision = true,
                    domainMatch = true
                )
            )
            assertTrue(simulateVerify(jsonStr))
            assertTrue(simulateVerify(jsonStr) { useAlternativeDomain = true })
            assertEquals(expectedDetail, simulateDetails(jsonStr))
            assertEquals(expectedDetail, simulateDetails(jsonStr) { useAlternativeDomain = true })
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "apk_package_name": "com.wusatosi.test"
                    }
                """.trimIndent()
            val expectedDetail = Either.right<ErrorCode, _>(
                V2ResponseDetail(true, "com.wusatosi.test") to V2Decision(
                    decision = true,
                    domainMatch = true
                )
            )
            assertTrue(simulateVerify(jsonStr))
            assertTrue(simulateVerify(jsonStr) { useAlternativeDomain = true })
            assertEquals(expectedDetail, simulateDetails(jsonStr))
            assertEquals(expectedDetail, simulateDetails(jsonStr) { useAlternativeDomain = true })
        }
        run {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "apk_package_name": "com.github"
                    }
                """.trimIndent()
            val expectedDetail = Either.right<ErrorCode, _>(
                V2ResponseDetail(true, "com.github") to V2Decision(
                    decision = true,
                    domainMatch = true
                )
            )
            assertTrue(simulateVerify(jsonStr))
            assertTrue(simulateVerify(jsonStr) { useAlternativeDomain = true })
            assertEquals(expectedDetail, simulateDetails(jsonStr))
            assertEquals(expectedDetail, simulateDetails(jsonStr) { useAlternativeDomain = true })
        }
    }

    @Test
    fun success_domainMatch() = runBlocking {
        @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
        val expectedDetail =
            Either.right<ErrorCode, _>(
                V2ResponseDetail(true, "wusatosi.com") to V2Decision(
                    decision = true,
                    domainMatch = true
                )
            )

        suspend fun testDomainMatch(config: RecaptchaV2Config.() -> Unit) {
            assertTrue(simulateVerify(jsonStr, block = config))
            assertEquals(expectedDetail, simulateDetails(jsonStr, block = config))
        }

        testDomainMatch {
            allowDomain("wusatosi.com")
        }
        testDomainMatch {
            allowDomain("wusatosi.com", "google.com")
        }
        testDomainMatch {
            allowDomains(listOf("wusatosi.com", "google.com"))
        }
    }

    @Test
    fun failure_response() =
        runBlocking {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
            val expectedDetail =
                Either.right<ErrorCode, _>(
                    V2ResponseDetail(false, "wusatosi.com") to V2Decision(
                        decision = false,
                        domainMatch = true
                    )
                )

            assertFalse(simulateVerify(jsonStr))
            assertEquals(expectedDetail, simulateDetails(jsonStr))
        }

    @Test
    fun failure_invalidTokenByPreCheck() =
        runBlocking {
            // Should not matter
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
            val expectedDetail = Either.left<_, Pair<V2ResponseDetail, V2Decision>>(ErrorCode.InvalidToken)

            assertFalse(simulateVerify(jsonStr, "阿"))
            assertEquals(expectedDetail, simulateDetails(jsonStr, "阿"))
            assertFalse(simulateVerify(jsonStr, ""))
            assertEquals(expectedDetail, simulateDetails(jsonStr, ""))
        }

    @Test
    fun failure_domainMismatch() =
        runBlocking {
            @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
            val expectedDetail =
                Either.right<ErrorCode, _>(
                    V2ResponseDetail(true, "wusatosi.com") to V2Decision(
                        decision = false,
                        domainMatch = false
                    )
                )

            suspend fun testDomainMismatch(config: RecaptchaV2Config.() -> Unit) {
                assertFalse(simulateVerify(jsonStr, block = config))
                assertEquals(expectedDetail, simulateDetails(jsonStr, block = config))
            }

            testDomainMismatch {
                allowDomain("google.com")
            }

            testDomainMismatch {
                allowDomain("google.com", "github.com")
            }

            testDomainMismatch {
                allowDomains(listOf("google.com", "github.com"))
            }
        }

    @Test
    fun invalidSiteSecret() =
        runBlocking {
            @Language("JSON") val singleError = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateVerify(singleError) }
            assertThrows<InvalidSiteKeyException> { simulateDetails(singleError) }

            @Language("JSON") val twoError = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-response", "invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateVerify(twoError) }
            assertThrows<InvalidSiteKeyException> { simulateDetails(twoError) }

            @Language("JSON") val threeError = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-response", "invalid-input-secret", "timeout-or-duplicate"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulateVerify(threeError) }
            assertThrows<InvalidSiteKeyException> { simulateDetails(threeError) }

            Unit
        }

    @Test
    fun failure_invalidToken() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "error-codes": ["invalid-input-response"]
                }
            """.trimIndent()
        assertFalse(simulateVerify(jsonStr))
        val either = simulateDetails(jsonStr)
        assertTrue(either is Left)
        assertEquals(ErrorCode.InvalidToken, either.left)
    }

    @Test
    fun failure_TimeoutOrDup() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "error-codes": ["timeout-or-duplicate"]
                }
            """.trimIndent()
        assertFalse(simulateVerify(jsonStr))
        val either = simulateDetails(jsonStr)
        assertTrue(either is Left)
        assertEquals(ErrorCode.TimeOrDuplicatedToken, either.left)
    }

}