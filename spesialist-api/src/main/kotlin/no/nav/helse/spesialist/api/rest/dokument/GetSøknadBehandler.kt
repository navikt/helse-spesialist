package no.nav.helse.spesialist.api.rest.dokument

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknad
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetSøknadBehandler(
    private val dokumenthåndterer: Dokumenthåndterer,
) : GetBehandler<Personer.AktørId.Dokumenter.DokumentId.Søknad, ApiSoknad> {
    override fun behandle(
        resource: Personer.AktørId.Dokumenter.DokumentId.Søknad,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiSoknad> {
        val fødselsnumre =
            transaksjon.legacyPersonRepository.finnFødselsnumre(aktørId = resource.parent.parent.parent.aktørId).toSet()

        val dokument =
            dokumenthåndterer.håndter(
                dokumentDao = transaksjon.dokumentDao,
                fødselsnummer = fødselsnumre.first(),
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.SØKNAD.name,
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
                code(HttpStatusCode.NotFound) {
                    description = "Fant ikke søknad eller saksbehandler mangler tilgang til personen som eier søknaden."
                }
                code(HttpStatusCode.RequestTimeout) {
                    description = "Henting av søknad tok for lang tid."
                }
            }
        }
    }
}
