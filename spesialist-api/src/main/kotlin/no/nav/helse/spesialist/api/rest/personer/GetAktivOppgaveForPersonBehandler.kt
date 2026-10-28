package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiAktivOppgave
import no.nav.helse.spesialist.api.rest.ApiAktivOppgaveForPerson
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId

class GetAktivOppgaveForPersonBehandler : GetBehandler<Personer.PersonPseudoId.AktivOppgave, ApiAktivOppgaveForPerson, ApiGetAktivOppgaveForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.AktivOppgave,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiAktivOppgaveForPerson, ApiGetAktivOppgaveForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetAktivOppgaveForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetAktivOppgaveForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val oppgave = kallKontekst.transaksjon.oppgaveRepository.finnAktivForPersonOrNull(person.id)
            RestResponse.OK(
                ApiAktivOppgaveForPerson(
                    aktivOppgave =
                        oppgave?.let {
                            ApiAktivOppgave(
                                id = it.id.value.toString(),
                                vedtaksperiodeId = it.vedtaksperiodeId.value,
                            )
                        },
                ),
            )
        }
}

enum class ApiGetAktivOppgaveForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
