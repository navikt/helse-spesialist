package no.nav.helse.spesialist.api.rest.dokument

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.ApiSoknad
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetSoknadBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.AktørId.Dokumenter.DokumentId.Soknad, ApiSoknad> {
    override fun behandle(
        resource: Personer.AktørId.Dokumenter.DokumentId.Soknad,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiSoknad> {
        val fødselsnumre =
            transaksjon.legacyPersonRepository.finnFødselsnumre(aktørId = resource.parent.parent.parent.aktørId).toSet()

        val dokument =
            dokumentMediator.håndter(
                dokumentDao = transaksjon.dokumentDao,
                fødselsnummer = fødselsnumre.first(),
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.SØKNAD,
            )

        val fødselsnummerForSøknad = dokument.get("fnr").asText()

        bekreftTilgangTilPerson(
            fødselsnummer = fødselsnummerForSøknad,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpNotFound,
        )

        return RestResponse.ok(
            dokument.tilSøknad(),
        )
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Dokumenter")
            operationId = operationIdBasertPåKlassenavn()
            response {
                code(HttpStatusCode.OK) {
                    description = "Sykepengesøknad med dokumentId."
                    body<ApiSoknad>()
                }
            }
        }
    }
}
