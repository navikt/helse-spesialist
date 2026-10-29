package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPaVent
import no.nav.helse.spesialist.api.rest.ApiPaVentForPerson
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId

class GetPaVentForPersonBehandler : GetBehandler<Personer.PersonPseudoId.PaVent, ApiPaVentForPerson, ApiGetPaVentForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.PaVent,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPaVentForPerson, ApiGetPaVentForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetPaVentForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetPaVentForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val paVent =
                kallKontekst.transaksjon.oppgaveRepository
                    .finnAktivForPersonOrNull(person.id)
                    ?.let { kallKontekst.transaksjon.påVentRepository.finnFor(it.vedtaksperiodeId) }
                    ?.let { ApiPaVent(frist = it.frist, oid = it.saksbehandlerOid.value) }
            RestResponse.OK(ApiPaVentForPerson(paVent = paVent))
        }
}

enum class ApiGetPaVentForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
