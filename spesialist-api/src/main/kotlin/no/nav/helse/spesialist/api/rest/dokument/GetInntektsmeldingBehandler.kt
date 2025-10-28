package no.nav.helse.spesialist.api.rest.dokument

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetInntektsmeldingBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.AktørId.Dokumenter.DokumentId.Inntektsmelding, ApiDokumentInntektsmelding> {
    override fun behandle(
        resource: Personer.AktørId.Dokumenter.DokumentId.Inntektsmelding,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiDokumentInntektsmelding> {
        val fødselsnumre =
            transaksjon.legacyPersonRepository.finnFødselsnumre(aktørId = resource.parent.parent.parent.aktørId).toSet()

        val dokument =
            dokumentMediator.håndter(
                dokumentDao = transaksjon.dokumentDao,
                fødselsnummer = fødselsnumre.first(),
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.INNTEKTSMELDING.name,
            )

        val fødselsnummerForIM =
            dokument
                .get("arbeidstakerFnr")
                .asText()
                .takeUnless { it.isEmpty() }
                ?.let { setOf(it) } ?: emptySet()
        val aktørIdForIM = dokument.get("arbeidstakerAktorId").asText()

        val fødselsnummreForIM =
            fødselsnummerForIM + transaksjon.legacyPersonRepository.finnFødselsnumre(aktørIdForIM).toSet()

        if (fødselsnummreForIM.isEmpty()) throw HttpNotFound("IM mangler fødselsnummer og aktørId")

        fødselsnummreForIM.forEach { fødselsnummer ->
            bekreftTilgangTilPerson(
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
                feilSupplier = ::HttpNotFound,
            )
        }

        return RestResponse.ok(
            dokument.tilInntektsmelding(),
        )
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Dokumenter")
            operationId = operationIdBasertPåKlassenavn()
            response {
                code(HttpStatusCode.OK) {
                    description = "Inntektsmelding med dokumentId."
                    body<ApiDokumentInntektsmelding>()
                }
            }
        }
    }
}
