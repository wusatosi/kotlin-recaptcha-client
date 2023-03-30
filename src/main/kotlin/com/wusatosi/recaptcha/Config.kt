package com.wusatosi.recaptcha

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

sealed class RecaptchaConfig {
    var engine: HttpClientEngine = CIO.create()
    internal var hostList: MutableList<String> = mutableListOf()
    var useAlternativeDomain: Boolean = false

    fun allowHost(vararg host: String) {
        hostList.addAll(host)
    }

    fun allowHosts(hosts: Iterable<String>) {
        hostList.addAll(hosts)
    }
}

// Internal base representation of the pure RecaptchaConfig
internal class BaseConfig : RecaptchaConfig()

class RecaptchaV2Config : RecaptchaConfig()

class RecaptchaV3Config : RecaptchaConfig() {

    internal var actionToScoreThreshold: (String) -> Double = { scoreThreshold }
    var scoreThreshold = 0.5

    fun limitedActions(vararg action: String) {
        actionToScoreThreshold = {
            if (it in action)
                scoreThreshold
            else
                5.0
        }
    }

    fun mapActionToThreshold(block: (String) -> Double) {
        actionToScoreThreshold = block
    }
}

sealed class Either<L, R> {
    abstract val right: R
    abstract val left: L

    companion object {
        fun <L, R> left(left: L): Either<L, R> = Left(left)
        fun <L, R> right(right: R): Either<L, R> = Right(right)
    }
}

data class Left<L, R>(override val left: L) : Either<L, R>() {
    override val right: R
        get() = throw IllegalStateException("No Right Value")
}

data class Right<L, R>(override val right: R) : Either<L, R>() {
    override val left: L
        get() = throw IllegalStateException("No Left Value")
}