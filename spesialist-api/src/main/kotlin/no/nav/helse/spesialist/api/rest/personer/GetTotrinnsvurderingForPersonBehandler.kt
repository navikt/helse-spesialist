package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiTotrinnsvurdering
import no.nav.helse.spesialist.api.rest.ApiTotrinnsvurderingForPerson
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER

class GetTotrinnsvurderingForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Totrinnsvurdering, ApiTotrinnsvurderingForPerson, ApiGetTotrinnsvurderingForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Totrinnsvurdering,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiTotrinnsvurderingForPerson, ApiGetTotrinnsvurderingForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetTotrinnsvurderingForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetTotrinnsvurderingForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val totrinnsvurdering = kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPersonOrNull(person.id.value)
            RestResponse.OK(ApiTotrinnsvurderingForPerson(totrinnsvurdering = totrinnsvurdering?.tilApiTotrinnsvurdering()))
        }
}

internal fun Totrinnsvurdering.tilApiTotrinnsvurdering() =
    ApiTotrinnsvurdering(
        erRetur = tilstand == AVVENTER_SAKSBEHANDLER && saksbehandler != null,
        saksbehandler = saksbehandler?.value,
        beslutter = beslutter?.value,
        erBeslutteroppgave = tilstand == AVVENTER_BESLUTTER,
    )

enum class ApiGetTotrinnsvurderingForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
