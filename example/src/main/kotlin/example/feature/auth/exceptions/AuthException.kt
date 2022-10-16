package example.feature.auth.exceptions

open class AuthException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
