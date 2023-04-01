package com.wusatosi.recaptcha

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

sealed class RecaptchaConfig {
    internal var hostList: MutableList<String> = mutableListOf()

    /**
     * The underlying HttpClientEngine Ktor uses to transact network requests.
     * CIO as default.
     */
    var engine: HttpClientEngine = CIO.create()

    /**
     * Use recaptcha.net instead of google.com to verify the token.
     * This is useful if google.com is not accessible to the client.
     *
     * This endpoint is referred in Google's documentation [here](https://developers.google.com/recaptcha/docs/faq)
     */
    var useAlternativeDomain: Boolean = false

    /**
     * Verify that the recaptcha is solved under given domain(s).
     * We accept all domains by default.
     */
    fun allowDomain(vararg domain: String) {
        hostList.addAll(domain)
    }

    /**
     * Verify that the recaptcha is solved under given domain(s).
     * We accept all domains by default.
     */
    fun allowDomains(domain: Iterable<String>) {
        hostList.addAll(domain)
    }
}

// Internal base representation of the pure RecaptchaConfig
internal class BaseConfig : RecaptchaConfig()

/**
 * The configuration of a [com.wusatosi.recaptcha.v2.RecaptchaV2Client]
 */
class RecaptchaV2Config : RecaptchaConfig()

/**
 * The configuration of a [com.wusatosi.recaptcha.v3.RecaptchaV3Client]
 */
class RecaptchaV3Config : RecaptchaConfig() {

    private val limitedActions = mutableListOf<String>()

    internal var actionToScoreThreshold: (String) -> Double = {
        if (limitedActions.isEmpty() || it in limitedActions)
            scoreThreshold
        else
            5.0
    }

    /**
     * The score threshold the verification score is tested against.
     */
    var scoreThreshold = 0.5

    /**
     * Limits the verifiable action(s).
     */
    fun limitedActions(vararg action: String) {
        limitedActions.addAll(action)
    }

    /**
     * Configures the client so that it will compute a score threshold in respect to the
     * [action](https://developers.google.com/recaptcha/docs/v3#actions) associated with the verification.
     */
    fun mapActionToThreshold(block: (String) -> Double) {
        actionToScoreThreshold = block
    }
}
