package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiOpptegnelse
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Sekvensnummer
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetOpptegnelserForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Opptegnelser, List<ApiOpptegnelse>, ApiGetOpptegnelserForPersonErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.Opptegnelser,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiOpptegnelse>, ApiGetOpptegnelserForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetOpptegnelserForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetOpptegnelserForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(resource, person, kallKontekst) }

    private fun behandleForPerson(
        resource: Personer.PersonPseudoId.Opptegnelser,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiOpptegnelse>, ApiGetOpptegnelserForPersonErrorCode> {
        val opptegnelser =
            kallKontekst.transaksjon.opptegnelseRepository
                .finnAlleForPersonEtter(
                    opptegnelseId = Sekvensnummer(resource.etterSekvensnummer),
                    personIdentitetsnummer = person.id,
                ).map { opptegnelse ->
                    ApiOpptegnelse(
                        sekvensnummer = opptegnelse.id().value,
                        type =
                            opptegnelse.type.let {
                                when (it) {
                                    Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING -> ApiOpptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING
                                    Opptegnelse.Type.UTBETALING_ANNULLERING_FEILET -> ApiOpptegnelse.Type.UTBETALING_ANNULLERING_FEILET
                                    Opptegnelse.Type.UTBETALING_ANNULLERING_OK -> ApiOpptegnelse.Type.UTBETALING_ANNULLERING_OK
                                    Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV -> ApiOpptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV
                                    Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE -> ApiOpptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE
                                    Opptegnelse.Type.REVURDERING_AVVIST -> ApiOpptegnelse.Type.REVURDERING_AVVIST
                                    Opptegnelse.Type.REVURDERING_FERDIGBEHANDLET -> ApiOpptegnelse.Type.REVURDERING_FERDIGBEHANDLET
                                    Opptegnelse.Type.PERSONDATA_OPPDATERT -> ApiOpptegnelse.Type.PERSONDATA_OPPDATERT
                                }
                            },
                    )
                }.sortedByDescending { it.sekvensnummer }

        loggInfo(
            "Hentet ${opptegnelser.size} opptegnelser" +
                opptegnelser.maxOfOrNull { it.sekvensnummer }?.let { " (siste sekvensnummer: $it)" }.orEmpty(),
        )

        return RestResponse.OK(opptegnelser)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Opptegnelser")
        }
    }
}

enum class ApiGetOpptegnelserForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
