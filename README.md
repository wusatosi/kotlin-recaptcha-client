# kotlin-recaptcha-client

[![codecov](https://codecov.io/github/wusatosi/kotlin-recaptcha-client/branch/master/graph/badge.svg?token=MlC5HZqSU2)](https://codecov.io/github/wusatosi/kotlin-recaptcha-client)

kotlin-recaptcha-client is a kotlin library for server side reCAPTCHA verification,
it supports both recaptcha v2 and recaptcha v3.

"reCAPTCHA is a free CAPTCHA service that protects websites from spam and abuse."

- reCAPTCHA: https://www.google.com/recaptcha/about/
- Version: 1.1.0-beta
- License: MIT, see [LICENSE](LICENSE)

## Installation

WIP

## Basic Usage

For v2 client:

```kotlin
import com.wusatosi.recaptcha.v2.RecaptchaV2Client

val v2Client = RecaptchaV2Client.create("your site secret")
runBlocking {
    // RecaptchaV2Client.verify is a suspend function
    val result: Boolean = v2Client.verify(token)

    // ...
}
v2Client.close()
```

For v3 client:

```kotlin
import com.wusatosi.recaptcha.v3.RecaptchaV3Client

val v3Client = RecaptchaV3Client.create("your site secret")
runBlocking {
    // RecaptchaV3Client.verify is a suspend function
    val result: Double = v3Client.verify(token)

    // ...
}
v3Client.close()
```

## Configuring the client

### Domain verification

Both V2 Client and V3 Client can be configured to restrict access to the website/ apk package
for which the captcha was solved. The domain name is not verified by default.

```kotlin
RecaptchaV2Client.create("your site secret") {
    // Website domain:
    allowDomain("wusatosi.com", "google.com")
    allowDomains(listOf("github.com", "localhost"))

    // APK package domain:
    allowDomain("com.wusatosi.test")
}
```

### Score Threshold and Action Verification (v3 only)

For v3 client, you can config a score threshold for verification.

```kotlin
RecaptchaV3Client.create("your site secret") {
    scoreThreshold = 0.8
}
```

You can limit the scope of actions you would like to limit.

```kotlin
RecaptchaV3Client.create("your site secret") {
    limitedActions("login")
    limitedActions("interact", "publish")
}
```

You can also supply a lambda that produces a score threshold depending on which action is reported.

```kotlin
RecaptchaV3Client.create("your site secret") {
    mapActionToThreshold {
        when (it) {
            // a higher threshold to essential operations
            "login" -> 0.8
            // a lower threshold to non-essential operations
            "post" -> 0.5
            // We reject any other actions
            else -> 5.0
        }
    }
}
```

## Contributing

[Issues](https://github.com/wusatosi/kotlin-recaptcha-client/issues/new) and pull requests are all always welcome

## Uses:

* [Kotlin-coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [Ktor](https://ktor.io/docs/welcome.html) (http client)
* [Gson](https://github.com/google/gson)

## License

[MIT](https://choosealicense.com/licenses/mit/)
