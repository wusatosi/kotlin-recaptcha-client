package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import com.wusatosi.recaptcha.v3.RecaptchaV3Client.V3Decision
import com.wusatosi.recaptcha.v3.RecaptchaV3Client.V3ResponseDetail
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RecaptchaV3Test {

    private suspend fun simulateVerify(
        jsonStr: String,
        token: String = "token",
        block: RecaptchaV3Config.() -> Unit = {}
    ): Boolean {
        return RecaptchaV3Client.create("s") {
            block(this)
            engine = MockEngine {
                respondOk(jsonStr)
            }
        }.use { it.verify(token, "") }
    }

    private suspend fun simulateDetail(
        jsonStr: String,
        token: String = "token",
        block: RecaptchaV3Config.() -> Unit = {}
    ): Either<ErrorCode, Pair<V3ResponseDetail, V3Decision>> {
        return RecaptchaV3Client.create("s") {
            block(this)
            engine = MockEngine {
                respondOk(jsonStr)
            }
        }.use { it.getDetailedResponse(token) }
    }

    @Test
    fun creation(): Unit = runBlocking {
        RecaptchaV3Client.create("secret")
    }

    @Test
    fun creationFailure_preCheck(): Unit = runBlocking {
        assertThrows<InvalidSiteKeyException> { RecaptchaV3Client.create(" ") }
        assertThrows<InvalidSiteKeyException> { RecaptchaV3Client.create("阿") }
    }

    @Test
    fun success(): Unit = runBlocking {
        @Language("JSON") val jsonStr = """
            {
              "success": true,
              "action": "click",
              "score": 0.9,
              "challenge_ts": "2023-03-28T22:10:10Z",
              "hostname": "wusatosi.com"
            }
        """.trimIndent()
        val expectedDetail = Either.right<ErrorCode, _>(
            V3ResponseDetail(
                true,
                "wusatosi.com",
                0.9,
                "click"
            ) to V3Decision(
                decision = true,
                hostMatch = true,
                suggestedThreshold = 0.5
            )
        )
        assertTrue(simulateVerify(jsonStr))
        assertEquals(expectedDetail, simulateDetail(jsonStr))
    }

    @Test
    fun customThreshold_success(): Unit = runBlocking {
        @Language("JSON") val jsonStr = """
            {
              "success": true,
              "action": "click",
              "score": 0.3,
              "challenge_ts": "2023-03-28T22:10:10Z",
              "hostname": "wusatosi.com"
            }
        """.trimIndent()
        val expectedDetail = Either.right<ErrorCode, _>(
            V3ResponseDetail(
                true,
                "wusatosi.com",
                0.3,
                "click"
            ) to V3Decision(
                decision = true,
                hostMatch = true,
                suggestedThreshold = 0.2
            )
        )
        assertTrue(simulateVerify(jsonStr) { scoreThreshold = 0.2 })
        assertEquals(expectedDetail, simulateDetail(jsonStr) { scoreThreshold = 0.2 })
    }

    @Test
    fun customThreshold_fail(): Unit = runBlocking {
        @Language("JSON") val jsonStr = """
            {
              "success": true,
              "action": "click",
              "score": 0.6,
              "challenge_ts": "2023-03-28T22:10:10Z",
              "hostname": "wusatosi.com"
            }
        """.trimIndent()
        val expectedDetail = Either.right<ErrorCode, _>(
            V3ResponseDetail(
                true,
                "wusatosi.com",
                0.6,
                "click"
            ) to V3Decision(
                decision = false,
                hostMatch = true,
                suggestedThreshold = 0.7
            )
        )
        assertFalse(simulateVerify(jsonStr) { scoreThreshold = 0.7 })
        assertEquals(expectedDetail, simulateDetail(jsonStr) { scoreThreshold = 0.7 })
    }

    @Test
    fun limitedAction_success(): Unit = runBlocking {
        @Language("JSON") val jsonStr = """
            {
              "success": true,
              "action": "click",
              "score": 0.6,
              "challenge_ts": "2023-03-28T22:10:10Z",
              "hostname": "wusatosi.com"
            }
        """.trimIndent()

        val expectedDetail = Either.right<ErrorCode, _>(
            V3ResponseDetail(
                true,
                "wusatosi.com",
                0.6,
                "click"
            ) to V3Decision(
                decision = true,
                hostMatch = true,
                suggestedThreshold = 0.5
            )
        )

        suspend fun testAction(block: RecaptchaV3Config.() -> Unit) {
            assertTrue(simulateVerify(jsonStr, block = block))
            assertEquals(expectedDetail, simulateDetail(jsonStr, block = block))
        }

        testAction { limitedActions("click") }
        testAction { limitedActions("click", "login") }
        testAction {
            limitedActions("click")
            limitedActions("login")
        }
    }

    @Test
    fun limitedAction_failure(): Unit = runBlocking {
        @Language("JSON") val jsonStr = """
            {
              "success": true,
              "action": "click",
              "score": 0.6,
              "challenge_ts": "2023-03-28T22:10:10Z",
              "hostname": "wusatosi.com"
            }
        """.trimIndent()
        val expectedDetail = Either.right<ErrorCode, _>(
            V3ResponseDetail(
                true,
                "wusatosi.com",
                0.6,
                "click"
            ) to V3Decision(
                decision = false,
                hostMatch = true,
                suggestedThreshold = 5.0
            )
        )

        suspend fun testAction(block: RecaptchaV3Config.() -> Unit) {
            assertEquals(expectedDetail, simulateDetail(jsonStr, block = block))
            assertFalse(simulateVerify(jsonStr, block = block))
        }

        testAction { limitedActions("login") }
        testAction { limitedActions("logout", "login") }
    }

    @Test
    fun customThresholdMapping() = runBlocking {
        val config: RecaptchaV3Config.() -> Unit = {
            mapActionToThreshold {
                when (it) {
                    "click" -> 0.6
                    "login" -> 0.7
                    else -> 5.0
                }
            }
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "action": "click",
                  "score": 0.65,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()

            val expectedDetail = Either.right<ErrorCode, _>(
                V3ResponseDetail(
                    true,
                    "wusatosi.com",
                    0.65,
                    "click"
                ) to V3Decision(
                    decision = true,
                    hostMatch = true,
                    suggestedThreshold = 0.6
                )
            )

            assertTrue(simulateVerify(jsonStr, block = config))
            assertEquals(expectedDetail, simulateDetail(jsonStr, block = config))
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "action": "click",
                  "score": 0.55,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()

            val expectedDetail = Either.right<ErrorCode, _>(
                V3ResponseDetail(
                    true,
                    "wusatosi.com",
                    0.55,
                    "click"
                ) to V3Decision(
                    decision = false,
                    hostMatch = true,
                    suggestedThreshold = 0.6
                )
            )

            assertFalse(simulateVerify(jsonStr, block = config))
            assertEquals(expectedDetail, simulateDetail(jsonStr, block = config))
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "action": "login",
                  "score": 0.8,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()

            val expectedDetail = Either.right<ErrorCode, _>(
                V3ResponseDetail(
                    true,
                    "wusatosi.com",
                    0.8,
                    "login"
                ) to V3Decision(
                    decision = true,
                    hostMatch = true,
                    suggestedThreshold = 0.7
                )
            )

            assertTrue(simulateVerify(jsonStr, block = config))
            assertEquals(expectedDetail, simulateDetail(jsonStr, block = config))
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "action": "login",
                  "score": 0.6,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()

            val expectedDetail = Either.right<ErrorCode, _>(
                V3ResponseDetail(
                    true,
                    "wusatosi.com",
                    0.6,
                    "login"
                ) to V3Decision(
                    decision = false,
                    hostMatch = true,
                    suggestedThreshold = 0.7
                )
            )

            assertFalse(simulateVerify(jsonStr, block = config))
            assertEquals(expectedDetail, simulateDetail(jsonStr, block = config))
        }

    }

    @Test
    fun malformed_failure_allEmpty() = runBlocking {
        @Language("JSON") val jsonStr = """
            {
              "success": false
            }
        """.trimIndent()

        assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
        assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }

        Unit
    }

    @Test
    fun failure_invalidToken() = runBlocking {
        // Doesn't matter
        @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "action": "login",
                  "score": 0.6,
                  "challenge_ts": "2023-03-28T22:10:10Z",
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()

        suspend fun attempt(token: String) {
            assertFalse(simulateVerify(jsonStr, token))
            val either = simulateDetail(jsonStr, token)
            assertTrue(either is Left)
            assertEquals(ErrorCode.InvalidToken, either.left)
        }

        attempt(" ")
        attempt("阿")
    }

    @Test
    fun failure_invalidToken_preCheck() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "error-codes": ["invalid-input-response"]
                }
        """.trimIndent()

        assertFalse(simulateVerify(jsonStr))
        val either = simulateDetail(jsonStr)
        assertTrue(either is Left)
        assertEquals(ErrorCode.InvalidToken, either.left)
    }

    @Test
    fun failure_timeoutDuplicate() = runBlocking {
        @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "error-codes": ["timeout-or-duplicate"]
                }
        """.trimIndent()

        assertFalse(simulateVerify(jsonStr))
        val either = simulateDetail(jsonStr)
        assertTrue(either is Left)
        assertEquals(ErrorCode.TimeOrDuplicatedToken, either.left)
    }

    @Test
    fun malformed_missingBoth() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "hostname": "wusatosi.com"
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }
        }

        Unit
    }

    @Test
    fun malformed_missingScore() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "action": "click"
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "hostname": "wusatosi.com",
                  "action": "click"
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }
        }

        Unit
    }

    @Test
    fun malformed_missingAction() = runBlocking {
        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "score": 0.0
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }
        }

        run {
            @Language("JSON") val jsonStr = """
                {
                  "success": false,
                  "hostname": "wusatosi.com",
                  "score": 0.0
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(jsonStr) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(jsonStr) }
        }

        Unit
    }

    @Test
    fun malformed_invalidType() = runBlocking {
        run {
            @Language("JSON") val wrongScore = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "score": "0.0",
                  "action": ""
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(wrongScore) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(wrongScore) }
        }

        run {
            @Language("JSON") val wrongScore = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "score": "0.0",
                  "action": []
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(wrongScore) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(wrongScore) }
        }

        run {
            @Language("JSON") val wrongAction = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "score": 0.0,
                  "action": []
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(wrongAction) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(wrongAction) }
        }

        run {
            @Language("JSON") val wrongAction = """
                {
                  "success": true,
                  "hostname": "wusatosi.com",
                  "score": 0.0,
                  "action": false
                }
            """.trimIndent()
            assertThrows<UnexpectedJsonStructure> { simulateVerify(wrongAction) }
            assertThrows<UnexpectedJsonStructure> { simulateDetail(wrongAction) }
        }

        Unit
    }

}