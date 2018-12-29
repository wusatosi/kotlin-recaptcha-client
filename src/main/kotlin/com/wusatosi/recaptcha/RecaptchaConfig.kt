package com.wusatosi.recaptcha

object RecaptchaConfig {

    /**
     * Default value when remote returns "invalid-input-response", or token contains characters that
     * is not suitable in an URL string for v3 client
     */
    @set:Synchronized
    var DEFAULT_INVALIDATE_TOKEN_SCORE = -1.0

    /**
     * Default value when remote returns "timeout-or-duplicate" for v3 client
     */
    @set:Synchronized
    var DEFAULT_TIMEOUT_OR_DUPLICATE_SCORE = -2.0

}