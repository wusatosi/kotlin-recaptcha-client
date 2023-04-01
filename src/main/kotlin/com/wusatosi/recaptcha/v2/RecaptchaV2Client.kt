package com.wusatosi.recaptcha.v2

import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.internal.RecaptchaV2ClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter

interface RecaptchaV2Client : RecaptchaClient {

    /**
     * A Representation of the response for verifying V2 token.
     * This is in line with [Recaptcha's official documentation](https://developers.google.com/recaptcha/docs/verify).
     *
     * @property success Whether this request was a valid reCAPTCHA token for your site
     * @property domain The hostname of the site/ package name of the app where the reCAPTCHA was solved
     */
    data class V2ResponseDetail(
        val success: Boolean,
        val domain: String
    )

    /**
     * The representation of the decision V2 client made.
     *
     * @property decision The final decision of this verification, this is equivalent to the result of calling verify
     * @property domainMatch Whether the verifier's domain is within the allowed domain configured
     *  through RecaptchaV2Config at client's creation
     */
    data class V2Decision(
        val decision: Boolean,
        val domainMatch: Boolean
    )

    /**
     * Get detailed response of the verification. This includes Google's response to the verification request,
     * and the client's decision acting on top (in the case of V2, whether the domain matches the requirement).
     *
     * @param token The token user has provided
     * @param remoteIp The remote IP of the user, this field is optional,
     *  if an empty string is provided, it will not be included in the request.
     * @return Either the error code of the response, or the response from Google
     *  along with the client's suggested decision, see V2ResponseDetail and V2Decision
     *
     * @throws RecaptchaError This function will throw RecaptchaError when it either suffers IO problems,
     *  or if the response it receives is malformed.
     */
    @Throws(RecaptchaError::class)
    suspend fun getDetailedResponse(
        token: String,
        remoteIp: String = ""
    ): Either<ErrorCode, Pair<V2ResponseDetail, V2Decision>>

    companion object {

        /**
         * Create a V2 client
         *
         * @param secretKey The secret key for verification. You should find them on your recaptcha admin panel.
         * @param block A DSL style configuration of the V3 client, see RecaptchaV2Config.
         */
        fun create(
            secretKey: String,
            block: RecaptchaV2Config.() -> Unit = {}
        ): RecaptchaV2Client {
            if (!likelyValidRecaptchaParameter(secretKey))
                throw InvalidSiteKeyException

            val config = RecaptchaV2Config().apply(block)
            return RecaptchaV2ClientImpl(
                secretKey,
                config
            )
        }
    }

}