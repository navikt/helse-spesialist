package no.nav.helse.spesialist.api.rest.personer.dokumenter

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId

class GetInntektsmeldingBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.PersonPseudoId.Dokumenter.DokumentId.Inntektsmelding, ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.Dokumenter.DokumentId.Inntektsmelding,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> {
        val personId = resource.parent.parent.parent.pseudoId
        val pseudoId = PersonPseudoId.fraString(personId)
        val identitetsnummer =
            kallKontekst.transaksjon.personPseudoIdDao.hentIdentitetsnummer(pseudoId)
                ?: return RestResponse.Error(ApiGetInntektsmeldingErrorCode.PERSON_IKKE_FUNNET)

        val dokument =
            dokumentMediator.hentDokument(
                dokumentDao = kallKontekst.transaksjon.dokumentDao,
                fødselsnummer = identitetsnummer.value,
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.INNTEKTSMELDING,
            ) ?: return RestResponse.Error(ApiGetInntektsmeldingErrorCode.FANT_IKKE_DOKUMENT)

        val fødselsnummerForIM =
            dokument
                .get("arbeidstakerFnr")
                .asText()
                .takeUnless { it.isEmpty() }
                ?.let { setOf(it) } ?: emptySet()
        val aktørIdForIM =
            dokument
                .get("arbeidstakerAktorId")
                .asText()
                .takeUnless { it.isEmpty() }

        val fødselsnumreForIM =
            fødselsnummerForIM +
                aktørIdForIM
                    ?.let {
                        kallKontekst.transaksjon.legacyPersonRepository
                            .finnFødselsnumre(aktørIdForIM)
                            .toSet()
                    }.orEmpty()

        if (fødselsnumreForIM.isEmpty()) return RestResponse.Error(ApiGetInntektsmeldingErrorCode.MANGLER_FØDSELSNUMMER_OG_AKTØRID)
        if (identitetsnummer.value !in fødselsnumreForIM) return RestResponse.Error(ApiGetInntektsmeldingErrorCode.FANT_IKKE_DOKUMENT)

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = identitetsnummer,
                tilgangsgrupper = kallKontekst.tilgangsgrupper,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiGetInntektsmeldingErrorCode.MANGLER_TILGANG_TIL_PERSON)
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
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_DOKUMENT("Fant ikke inntektsmeldingdokument", HttpStatusCode.NotFound),
    MANGLER_FØDSELSNUMMER_OG_AKTØRID("IM mangler fødselsnummer og aktørId", HttpStatusCode.NotFound),
}
