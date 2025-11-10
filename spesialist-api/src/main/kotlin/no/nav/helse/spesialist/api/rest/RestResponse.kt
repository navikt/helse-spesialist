package no.nav.helse.spesialist.api.rest

sealed interface RestResponse<RESPONSE, ERROR : ApiErrorCode> {
    sealed interface Success<RESPONSE, ERROR : ApiErrorCode> : RestResponse<RESPONSE, ERROR>

    class OK<RESPONSE, ERROR : ApiErrorCode>(
        val body: RESPONSE,
    ) : Success<RESPONSE, ERROR>

    class Created<ERROR : ApiErrorCode>(
        val location: Any,
    ) : Success<Unit, ERROR>

    class NoContent<ERROR : ApiErrorCode> : Success<Unit, ERROR>

    class Error<RESPONSE, ERROR : ApiErrorCode>(
        val errorCode: ERROR,
        val detail: String? = null,
    ) : RestResponse<RESPONSE, ERROR>
}
