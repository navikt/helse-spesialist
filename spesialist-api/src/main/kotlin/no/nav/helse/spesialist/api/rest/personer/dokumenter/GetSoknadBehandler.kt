package no.nav.helse.spesialist.api.rest.personer.dokumenter

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSoknad
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetSoknadBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.PersonPseudoId.Dokumenter.DokumentId.Soknad, ApiSoknad, ApiGetSoknadErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.Dokumenter.DokumentId.Soknad,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiSoknad, ApiGetSoknadErrorCode> {
        val personId = resource.parent.parent.parent.pseudoId
        val pseudoId = PersonPseudoId.fraString(personId)
        val identitetsnummer =
            transaksjon.personPseudoIdDao.hentIdentitetsnummer(pseudoId)
                ?: return RestResponse.Error(ApiGetSoknadErrorCode.PERSON_IKKE_FUNNET)

        val dokument =
            dokumentMediator.hentDokument(
                dokumentDao = transaksjon.dokumentDao,
                fødselsnummer = identitetsnummer.value,
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.SØKNAD,
            ) ?: return RestResponse.Error(ApiGetSoknadErrorCode.FANT_IKKE_DOKUMENT)

        val fødselsnummerForSøknad = dokument.get("fnr").asText()

        if (fødselsnummerForSøknad != identitetsnummer.value) {
            logg.error("Fødselsnummer i søknad-dokumentet samsvarer ikke med identitetsnummer hentet via pseudo-id")
            return RestResponse.Error(ApiGetSoknadErrorCode.FANT_IKKE_DOKUMENT)
        }

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = identitetsnummer,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiGetSoknadErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        return RestResponse.OK(
            dokument.tilSøknad(),
        )
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Dokumenter")
        }
    }
}

enum class ApiGetSoknadErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_DOKUMENT("Fant ikke søknadsdokument", HttpStatusCode.NotFound),
}
