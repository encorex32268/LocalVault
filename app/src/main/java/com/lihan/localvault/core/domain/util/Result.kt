package com.lihan.localvault.core.domain.util

sealed interface Result<out D, out E : RootError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : RootError, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

inline fun <T, E : RootError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T, E : RootError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

inline fun <T, E : RootError> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

typealias EmptyResult<E> = Result<Unit, E>

fun <T, E : RootError> Result<T, E>.asEmptyResult(): EmptyResult<E> {
    return map { }
}

fun <T, E : RootError> Result<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map { }
}
