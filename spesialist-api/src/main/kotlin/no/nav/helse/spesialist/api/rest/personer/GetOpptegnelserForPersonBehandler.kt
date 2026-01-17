package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiOpptegnelse
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Sekvensnummer
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetOpptegnelserForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Opptegnelser, List<ApiOpptegnelse>, ApiGetOpptegnelserForPersonErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.Opptegnelser,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<List<ApiOpptegnelse>, ApiGetOpptegnelserForPersonErrorCode> {
        val personId = resource.parent.pseudoId
        val pseudoId = PersonPseudoId.fraString(personId)
        val identitetsnummer =
            transaksjon.personPseudoIdDao.hentIdentitetsnummer(pseudoId)
                ?: return RestResponse.Error(ApiGetOpptegnelserForPersonErrorCode.PERSON_IKKE_FUNNET)

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = identitetsnummer,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiGetOpptegnelserForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        return RestResponse.OK(
            transaksjon.opptegnelseRepository
                .finnAlleForPersonEtter(
                    opptegnelseId = Sekvensnummer(resource.etterSekvensnummer),
                    personIdentitetsnummer = identitetsnummer,
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
                }.sortedByDescending { it.sekvensnummer },
        )
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
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
