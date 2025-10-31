package no.nav.helse.spesialist.api.rest.dokument

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.harTilgangTilPerson
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetInntektsmeldingBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.AktørId.Dokumenter.DokumentId.Inntektsmelding, ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> {
    override fun behandle(
        resource: Personer.AktørId.Dokumenter.DokumentId.Inntektsmelding,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> {
        val fødselsnumre =
            transaksjon.legacyPersonRepository.finnFødselsnumre(aktørId = resource.parent.parent.parent.aktørId).toSet()

        val dokument =
            dokumentMediator.hentDokument(
                dokumentDao = transaksjon.dokumentDao,
                fødselsnummer = fødselsnumre.first(),
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.INNTEKTSMELDING,
            ) ?: return RestResponse.Error(ApiGetInntektsmeldingErrorCode.FANT_IKKE_DOKUMENT)

        val fødselsnummerForIM =
            dokument
                .get("arbeidstakerFnr")
                .asText()
                .takeUnless { it.isEmpty() }
                ?.let { setOf(it) } ?: emptySet()
        val aktørIdForIM = dokument.get("arbeidstakerAktorId").asText()

        val fødselsnummreForIM =
            fødselsnummerForIM + transaksjon.legacyPersonRepository.finnFødselsnumre(aktørIdForIM).toSet()

        if (fødselsnummreForIM.isEmpty()) return RestResponse.Error(ApiGetInntektsmeldingErrorCode.MANGLER_FØDSELSNUMMER_OG_AKTØRID)

        fødselsnummreForIM.forEach { fødselsnummer ->
            if (!harTilgangTilPerson(
                    fødselsnummer = fødselsnummer,
                    saksbehandler = saksbehandler,
                    tilgangsgrupper = tilgangsgrupper,
                    transaksjon = transaksjon,
                )
            ) {
                return RestResponse.Error(ApiGetInntektsmeldingErrorCode.MANGLER_TILGANG_TIL_PERSON)
            }
        }

        return RestResponse.OK(
            dokument.tilInntektsmelding(),
        )
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Dokumenter")
        }
    }
}

enum class ApiGetInntektsmeldingErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_DOKUMENT("Fant ikke inntektsmeldingdokument", HttpStatusCode.NotFound),
    MANGLER_FØDSELSNUMMER_OG_AKTØRID("IM mangler fødselsnummer og aktørId", HttpStatusCode.NotFound),
}
