package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode

enum class PersonErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    PERSON_IKKE_FUNNET(HttpStatusCode.NotFound, "Person ikke funnet"),
    PERSON_PSEUDO_ID_IKKE_FUNNET(HttpStatusCode.NotFound, "PersonPseudoId har utløpt (eller aldri eksistert)"),
}

class KallKontekstException(
    val errorCode: PersonErrorCode,
) : Exception()
