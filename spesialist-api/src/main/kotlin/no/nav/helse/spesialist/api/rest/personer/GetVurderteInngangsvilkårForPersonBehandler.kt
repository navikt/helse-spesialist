package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiAutomatiskVurdering
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiManuellVurdering
import no.nav.helse.spesialist.api.rest.`ApiSamlingAvVurderteInngangsvilkår`
import no.nav.helse.spesialist.api.rest.ApiVurdertInngangsvilkår
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.HistoriskeIdenterHenter
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.spillkar.SamlingAvVurderteInngangsvilkår
import no.nav.helse.spesialist.application.spillkar.VurdertInngangsvilkår
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.LocalDate

class GetVurderteInngangsvilkårForPersonBehandler(
    private val inngangsvilkårHenter: InngangsvilkårHenter,
    private val historiskeIdenterHenter: HistoriskeIdenterHenter,
) : GetBehandler<Personer.PersonPseudoId.VurderteInngangsvilkår.Skjæringstidspunkt, List<ApiSamlingAvVurderteInngangsvilkår>, ApiGetVurderteInngangsvilkårErrorCode> {
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
            behandleForPerson(person, resource.skjæringstidspunkt)
        }

    private fun behandleForPerson(
        person: Person,
        skjæringstidspunkt: LocalDate,
    ): RestResponse<List<ApiSamlingAvVurderteInngangsvilkår>, ApiGetVurderteInngangsvilkårErrorCode> {
        val historiskeIdenter = historiskeIdenterHenter.hentHistoriskeIdenter(person.id.value)

        val hentedeVurderinger: List<ApiSamlingAvVurderteInngangsvilkår> =
            inngangsvilkårHenter
                .hentInngangsvilkår(
                    personidentifikatorer = historiskeIdenter,
                    skjæringstidspunkt = skjæringstidspunkt,
                ).map { it.tilApiSamlingAvVurderteInngangsvilkår() }
        return RestResponse.OK(hentedeVurderinger)
    }
}

private fun SamlingAvVurderteInngangsvilkår.tilApiSamlingAvVurderteInngangsvilkår(): ApiSamlingAvVurderteInngangsvilkår =
    ApiSamlingAvVurderteInngangsvilkår(
        samlingAvVurderteInngangsvilkårId = samlingAvVurderteInngangsvilkårId,
        versjon = versjon,
        skjæringstidspunkt = skjæringstidspunkt,
        vurderteInngangsvilkår = vurderteInngangsvilkår.map { it.tilApiVurdertInngangsvilkår() },
    )

private fun VurdertInngangsvilkår.tilApiVurdertInngangsvilkår(): ApiVurdertInngangsvilkår =
    when (this) {
        is VurdertInngangsvilkår.AutomatiskVurdertInngangsvilkår ->
            ApiVurdertInngangsvilkår.Automatisk(
                vilkårskode = vilkårskode,
                vurderingskode = vurderingskode,
                tidspunkt = tidspunkt,
                automatiskVurdering =
                    ApiAutomatiskVurdering(
                        system = automatiskVurdering.system,
                        versjon = automatiskVurdering.versjon,
                        grunnlagsdata = automatiskVurdering.grunnlagsdata,
                    ),
            )
        is VurdertInngangsvilkår.ManueltVurdertInngangsvilkår ->
            ApiVurdertInngangsvilkår.Manuell(
                vilkårskode = vilkårskode,
                vurderingskode = vurderingskode,
                tidspunkt = tidspunkt,
                manuellVurdering =
                    ApiManuellVurdering(
                        navident = navident,
                        begrunnelse = begrunnelse,
                    ),
            )
    }

enum class ApiGetVurderteInngangsvilkårErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
