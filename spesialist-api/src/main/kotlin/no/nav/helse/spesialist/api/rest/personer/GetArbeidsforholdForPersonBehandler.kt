package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiArbeidsforholdForPerson
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId

class GetArbeidsforholdForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Arbeidsforhold, List<ApiArbeidsforholdForPerson>, ApiGetArbeidsforholdForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Arbeidsforhold,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiArbeidsforholdForPerson>, ApiGetArbeidsforholdForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetArbeidsforholdForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetArbeidsforholdForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            RestResponse.OK(
                kallKontekst.transaksjon.arbeidsforholdDao
                    .finnAlleForPerson(person.id.value)
                    .map {
                        ApiArbeidsforholdForPerson(
                            organisasjonsnummer = it.organisasjonsnummer,
                            stillingstittel = it.stillingstittel,
                            stillingsprosent = it.stillingsprosent,
                            startdato = it.startdato,
                            sluttdato = it.sluttdato,
                        )
                    },
            )
        }
}

enum class ApiGetArbeidsforholdForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
