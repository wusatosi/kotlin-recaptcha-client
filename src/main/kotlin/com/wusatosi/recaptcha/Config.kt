package com.wusatosi.recaptcha

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import java.lang.IllegalStateException

sealed class RecaptchaConfig {
    var engine: HttpClientEngine = CIO.create()
    var hostList: MutableList<String> = mutableListOf()
    var useAlternativeDomain: Boolean = false
}

class RecaptchaV2Config : RecaptchaConfig()

class RecaptchaV3Config : RecaptchaConfig() {

    internal var actionToScoreThreshold: (String) -> Double = { scoreThreshold }
    var scoreThreshold = 0.5

    fun acceptAllActions() {
        actionToScoreThreshold = { scoreThreshold }
    }

    fun limitedActions(vararg action: String) {
        actionToScoreThreshold = {
            if (it in action)
                scoreThreshold
            0.0
        }
    }

    fun mapActionToThreshold(block: (String) -> Double) {
        actionToScoreThreshold = block
    }
}

enum class Occupation {
    Left,
    Right
}

sealed class Either<L, R>(
    val occupation: Occupation
) {
    abstract val right: R
    abstract val left: L

    companion object {
        fun <L, R> left(left: L): Either<L, R> = LeftOnly(left)
        fun <L, R> right(left: R): Either<L, R> = RightOnly(left)
    }
}

internal class LeftOnly<L, R>(override val left: L) : Either<L, R>(Occupation.Left) {
    override val right: R
        get() = throw IllegalStateException("No Right Value")
}

internal class RightOnly<L, R>(override val right: R) : Either<L, R>(Occupation.Right) {
    override val left: L
        get() = throw IllegalStateException("No Left Value")
}