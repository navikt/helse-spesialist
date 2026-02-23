package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiManuellInngangsvilkårVurdering
import no.nav.helse.spesialist.api.rest.ApiPostManuelleInngangsvilkårVurderingerRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.InngangsvilkårInnsender
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.spillkar.ManuellInngangsvilkårVurdering
import no.nav.helse.spesialist.application.spillkar.ManuelleInngangsvilkårVurderinger
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.LocalDate

class PostVurderteInngangsvilkårForPersonBehandler(
    private val inngangsvilkårInnsender: InngangsvilkårInnsender,
) : PostBehandler<Personer.PersonPseudoId.VurderteInngangsvilkår.Skjæringstidspunkt, ApiPostManuelleInngangsvilkårVurderingerRequest, Unit, ApiPostVurderteInngangsvilkårErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Vilkårsvurderinger")
        }
    }

    override fun behandle(
        resource: Personer.PersonPseudoId.VurderteInngangsvilkår.Skjæringstidspunkt,
        request: ApiPostManuelleInngangsvilkårVurderingerRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostVurderteInngangsvilkårErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPostVurderteInngangsvilkårErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostVurderteInngangsvilkårErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(person, request, resource.skjæringstidspunkt)
        }

    private fun behandleForPerson(
        person: Person,
        request: ApiPostManuelleInngangsvilkårVurderingerRequest,
        skjæringstidspunkt: LocalDate,
    ): RestResponse<Unit, ApiPostVurderteInngangsvilkårErrorCode> {
        inngangsvilkårInnsender.sendManuelleVurderinger(
            ManuelleInngangsvilkårVurderinger(
                personidentifikator = person.id.value,
                skjæringstidspunkt = skjæringstidspunkt,
                versjon = request.versjon,
                vurderinger = request.vurderinger.map { it.tilManuellInngangsvilkårVurdering() },
            ),
        )
        return RestResponse.NoContent()
    }
}

private fun ApiManuellInngangsvilkårVurdering.tilManuellInngangsvilkårVurdering(): ManuellInngangsvilkårVurdering =
    ManuellInngangsvilkårVurdering(
        vilkårskode = vilkårskode,
        vurderingskode = vurderingskode,
        tidspunkt = tidspunkt,
        begrunnelse = begrunnelse,
    )

enum class ApiPostVurderteInngangsvilkårErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
