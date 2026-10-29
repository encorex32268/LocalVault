package com.lihan.localvault.core.domain.util

sealed interface DataError : Error {
    enum class Network : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        SERIALIZATION,
        UNKNOWN,
        CANCELED,
        FIREBASE_GET_DATA_FAILED
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        SERIALIZATION,
        CRYPTO_ERROR,
        INVALID_PASSPHRASE,
        CORRUPTED_DATA,
        UNKNOWN
    }
}
