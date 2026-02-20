package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.`ApiSamlingAvVurderteInngangsvilkår`
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetVurderteInngangsvilkårForPersonBehandler : GetBehandler<Personer.PersonPseudoId.VurderteInngangsvilkår.Skjæringstidspunkt, List<ApiSamlingAvVurderteInngangsvilkår>, ApiGetVurderteInngangsvilkårErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Vilkårsvurderinger")
        }
    }

    override fun behandle(
        resource: Personer.PersonPseudoId.VurderteInngangsvilkår.Skjæringstidspunkt,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiSamlingAvVurderteInngangsvilkår>, ApiGetVurderteInngangsvilkårErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetVurderteInngangsvilkårErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetVurderteInngangsvilkårErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(person, kallKontekst)
        }

    private fun behandleForPerson(
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiSamlingAvVurderteInngangsvilkår>, ApiGetVurderteInngangsvilkårErrorCode> {
        TODO("Not yet implemented")
    }
}

enum class ApiGetVurderteInngangsvilkårErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
