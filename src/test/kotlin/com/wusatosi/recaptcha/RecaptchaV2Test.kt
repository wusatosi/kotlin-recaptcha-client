package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RecaptchaV2Test {

    private suspend fun simulate(jsonStr: String, token: String = "token"): Boolean {
        val engine = MockEngine {
            respondOk(jsonStr)
        }
        val client = RecaptchaV2Client.create("site") { this.engine = engine }
        return client.use { it.verify(token) }
    }

    @Test
    fun success() =
        runBlocking {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
            assert(simulate(jsonStr))
        }

    @Test
    fun failure() =
        runBlocking {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
            assert(!simulate(jsonStr))
        }

    @Test
    fun failure_invalidTokenByPreCheck() =
        runBlocking {
            run {
                @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
                assert(!simulate(jsonStr, "阿"))
            }

            run {
                @Language("JSON") val jsonStr = """
                    {
                      "success": true,
                      "challenge_ts": "2023-03-28T22:10:10Z",
                      "hostname": "wusatosi.com"
                    }
                """.trimIndent()
                assert(!simulate(jsonStr, ""))
            }
        }

    @Test
    fun invalidSiteSecret() =
        runBlocking {
            @Language("JSON") val singleError = """
                {
                  "success": false,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com",
                  "error-codes": ["invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulate(singleError) }

            @Language("JSON") val twoError = """
                {
                  "success": false,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com",
                  "error-codes": ["invalid-input-response", "invalid-input-secret"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulate(twoError) }

            @Language("JSON") val threeError = """
                {
                  "success": false,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com",
                  "error-codes": ["invalid-input-response", "invalid-input-secret", "timeout-or-duplicate"]
                }
            """.trimIndent()
            assertThrows<InvalidSiteKeyException> { simulate(threeError) }

            Unit
        }

}