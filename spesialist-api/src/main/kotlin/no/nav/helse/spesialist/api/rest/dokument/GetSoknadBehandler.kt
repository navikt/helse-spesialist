package no.nav.helse.spesialist.api.rest.dokument

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSoknad
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.harTilgangTilPerson
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetSoknadBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.AktørId.Dokumenter.DokumentId.Soknad, ApiSoknad, ApiGetSoknadErrorCode> {
    override fun behandle(
        resource: Personer.AktørId.Dokumenter.DokumentId.Soknad,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiSoknad, ApiGetSoknadErrorCode> {
        val fødselsnumre =
            transaksjon.legacyPersonRepository.finnFødselsnumre(aktørId = resource.parent.parent.parent.aktørId).toSet()

        val dokument =
            dokumentMediator.hentDokument(
                dokumentDao = transaksjon.dokumentDao,
                fødselsnummer = fødselsnumre.first(),
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.SØKNAD,
            ) ?: return RestResponse.Error(ApiGetSoknadErrorCode.FANT_IKKE_DOKUMENT)

        val fødselsnummerForSøknad = dokument.get("fnr").asText()

        if (!harTilgangTilPerson(
                fødselsnummer = fødselsnummerForSøknad,
                saksbehandler = saksbehandler,
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
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_DOKUMENT("Fant ikke søknadsdokument", HttpStatusCode.NotFound),
}
