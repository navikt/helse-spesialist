package no.nav.helse.spesialist.api.rest.personer.dokumenter

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.DokumentMediator
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetInntektsmeldingBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.PersonPseudoId.Dokumenter.DokumentId.Inntektsmelding, ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.Dokumenter.DokumentId.Inntektsmelding,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetInntektsmeldingErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetInntektsmeldingErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(resource, person, kallKontekst) }

    private fun behandleForPerson(
        resource: Personer.PersonPseudoId.Dokumenter.DokumentId.Inntektsmelding,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiDokumentInntektsmelding, ApiGetInntektsmeldingErrorCode> {
        val dokument =
            dokumentMediator.hentDokument(
                dokumentDao = kallKontekst.transaksjon.dokumentDao,
                fødselsnummer = person.id.value,
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
        if (person.id.value !in fødselsnumreForIM) {
            return RestResponse.Error(
                ApiGetInntektsmeldingErrorCode.FANT_IKKE_DOKUMENT,
            )
        }

        val inntektsmelding = dokument.tilInntektsmelding()

        loggInfo("Hentet inntektsmeldingsdokument")

        return RestResponse.OK(inntektsmelding)
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
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_DOKUMENT("Fant ikke inntektsmeldingdokument", HttpStatusCode.NotFound),
    MANGLER_FØDSELSNUMMER_OG_AKTØRID("IM mangler fødselsnummer og aktørId", HttpStatusCode.NotFound),
}
