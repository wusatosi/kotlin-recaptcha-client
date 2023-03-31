# kotlin-recaptcha-client
[![codecov](https://codecov.io/github/wusatosi/kotlin-recaptcha-client/branch/master/graph/badge.svg?token=MlC5HZqSU2)](https://codecov.io/github/wusatosi/kotlin-recaptcha-client)

kotlin-recaptcha-client is an kotlin library for server side reCAPTCHA verification, supports both recaptcha v2 and
recaptcha v3

This client aims to be as simple as possible

"reCAPTCHA is a free CAPTCHA service that protects websites from spam and abuse."

- reCAPTCHA: https://www.google.com/recaptcha
- Version: 1.0.3
- License: MIT, see [LICENSE](LICENSE)

## Installation

Via package manager: 

Still in the phase of uploading to the maven central, not ready yet

## Usage

For v2 client:
```kotlin
import com.wusatosi.recaptcha.v2.RecaptchaV2Client

// val v2Client = RecaptchaClient.createV2("your site key...")
val v2Client = RecaptchaV2Client.create("your site key...")

runBlocking {
  // RecaptchaV2Client.verify is an suspend function
  val result: Boolean = v2Client.verify(token)
  
  // ...
}
```

For v3 client
```kotlin
import com.wusatosi.recaptcha.v3.RecaptchaV3Client

// val v3Client = RecaptchaClient.createV3("your site key...")
val v3Client = RecaptchaV3Client.create("your site key...")
runBlocking {

  // RecaptchaV3Client.getVerifyScore is an suspend function
  val result: Double = v3Client.getVerifyScore(token)
  
  // ...
}
```

Universal
```kotlin
import com.wusatosi.recaptcha.RecaptchaClient

val universalClient = RecaptchaClient.createUniversal("your site key...")

runBlocking {

    // RecaptchaClient.verify is an suspend function 
    val result: Boolean = universalClient.verify(token)
    
    // ...
}
```

Supports using "www.recaptcha.net" instead of "www.google.com" as the API endpoint, 
[for google side more documentation](https://developers.google.com/recaptcha/docs/faq), 
this can be enable via:
```kotlin
// V2
RecaptchaClient.createV2("your secret key...", true)
// RecaptchaClient.createV2("your secret key...", useRecaptchaDotNetEndPoint = true)

// V3
RecaptchaClient.createV3("your secret key...", true)
// RecaptchaClient.createV3("your secret key...", useRecaptchaDotNetEndPoint = true)
```

## Contributing
[Issues](https://github.com/wusatosi/kotlin-recaptcha-client/issues/new), pull requests, are all always welcome

## Uses:
* [Kotlin-coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [Ktor](https://ktor.io/docs/welcome.html) (http client)
* [Gson](https://github.com/google/gson)

## License
[MIT](https://choosealicense.com/licenses/mit/)
