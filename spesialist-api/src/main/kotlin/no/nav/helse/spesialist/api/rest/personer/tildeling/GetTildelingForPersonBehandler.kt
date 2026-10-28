package no.nav.helse.spesialist.api.rest.personer.tildeling

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiTildeling
import no.nav.helse.spesialist.api.rest.ApiTildelingForPerson
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId

class GetTildelingForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Tildeling, ApiTildelingForPerson, ApiGetTildelingForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Tildeling,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiTildelingForPerson, ApiGetTildelingForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetTildelingForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetTildelingForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val tildeling =
                kallKontekst.transaksjon.oppgaveRepository
                    .finnAktivForPersonOrNull(person.id)
                    ?.tildeltTil
                    ?.let(kallKontekst.transaksjon.saksbehandlerRepository::finnOrNull)
                    ?.let { ApiTildeling(navn = it.navn, epost = it.epost, oid = it.id.value) }
            RestResponse.OK(ApiTildelingForPerson(tildeling = tildeling))
        }
}

enum class ApiGetTildelingForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
