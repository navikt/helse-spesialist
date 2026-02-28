package no.nav.helse.spesialist.application

sealed interface Either<RESULT, ERROR> {
    data class Failure<RESULT, ERROR>(
        val error: ERROR,
    ) : Either<RESULT, ERROR>

    data class Success<RESULT, ERROR>(
        val result: RESULT,
    ) : Either<RESULT, ERROR>
}
