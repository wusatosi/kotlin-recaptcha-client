package com.wusatosi.recaptcha

/**
 * This is an Either-or container.
 * Only one value within [L] and [R] is contained.
 *
 * Accessing the value that's not consistent with its variant will cause us to throw an IllegalStateException.
 *
 * You should test if one is a [Left] value or [Right] value using "is".
 * ```kotlin
 * when (val either = /* ... */) {
 *  is Left -> /* ... */
 *  is Right -> /* ... */
 * }
 * ```
 *
 * This is basically a [Result] but with a non throwable exception variant.
 */
sealed class Either<L, R> {
    /**
     * The Right value
     */
    abstract val right: R

    /**
     * The Left value
     */
    abstract val left: L

    companion object {
        /**
         * Construct a [Either] object with only the left value.
         */
        fun <L, R> left(left: L): Either<L, R> = Left(left)

        /**
         * Construct a [Either] object with only the right value.
         */
        fun <L, R> right(right: R): Either<L, R> = Right(right)
    }
}

/**
 * The left variant of [Either]
 */
data class Left<L, R>(override val left: L) : Either<L, R>() {
    override val right: R
        get() = throw IllegalStateException("No Right Value")
}


/**
 * The right variant of [Either]
 */
data class Right<L, R>(override val right: R) : Either<L, R>() {
    override val left: L
        get() = throw IllegalStateException("No Left Value")
}