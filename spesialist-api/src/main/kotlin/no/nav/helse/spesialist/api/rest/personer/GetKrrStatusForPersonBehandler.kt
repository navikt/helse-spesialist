package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKrrStatus
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetKrrStatusForPersonBehandler(
    private val krrStatusHenter: Reservasjonshenter,
) : GetBehandler<Personer.PersonPseudoId.KrrStatus, ApiKrrStatus, ApiGetKrrStatusForPersonErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.KrrStatus,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiKrrStatus, ApiGetKrrStatusForPersonErrorCode> {
        val personId = resource.parent.pseudoId
        val pseudoId = PersonPseudoId.fraString(personId)
        val identitetsnummer =
            transaksjon.personPseudoIdDao.hentIdentitetsnummer(pseudoId)
                ?: return RestResponse.Error(ApiGetKrrStatusForPersonErrorCode.PERSON_IKKE_FUNNET)

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = identitetsnummer,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiGetKrrStatusForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val hentetStatus =
            runBlocking { krrStatusHenter.hentForPerson(identitetsnummer.value) }
                ?: return RestResponse.Error(ApiGetKrrStatusForPersonErrorCode.FEIL_VED_VIDERE_KALL)

        return RestResponse.OK(
            ApiKrrStatus(
                kanVarsles = hentetStatus.kanVarsles,
                reservert = hentetStatus.reservert,
            ),
        )
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("KRR")
        }
    }
}

enum class ApiGetKrrStatusForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FEIL_VED_VIDERE_KALL("Klarte ikke hente status fra Kontakt- og Reservasjonsregisteret", HttpStatusCode.InternalServerError),
}
