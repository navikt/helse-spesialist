package no.nav.helse.spesialist.api.rest.personer.dokumenter

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSoknad
import no.nav.helse.spesialist.api.rest.DokumentMediator
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetSoknadBehandler(
    private val dokumentMediator: DokumentMediator,
) : GetBehandler<Personer.PersonPseudoId.Dokumenter.DokumentId.Soknad, ApiSoknad, ApiGetSoknadErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.Dokumenter.DokumentId.Soknad,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiSoknad, ApiGetSoknadErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetSoknadErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetSoknadErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(resource, person, kallKontekst) }

    private fun behandleForPerson(
        resource: Personer.PersonPseudoId.Dokumenter.DokumentId.Soknad,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiSoknad, ApiGetSoknadErrorCode> {
        val identitetsnummer = person.id
        val dokument =
            dokumentMediator.hentDokument(
                dokumentDao = kallKontekst.transaksjon.dokumentDao,
                fødselsnummer = identitetsnummer.value,
                dokumentId = resource.parent.dokumentId,
                dokumentType = DokumentMediator.DokumentType.SØKNAD,
            ) ?: return RestResponse.Error(ApiGetSoknadErrorCode.FANT_IKKE_DOKUMENT)

        val fødselsnummerForSøknad = dokument.get("fnr").asText()

        if (fødselsnummerForSøknad != identitetsnummer.value) {
            logg.error("Fødselsnummer i søknad-dokumentet samsvarer ikke med identitetsnummer hentet via pseudo-id")
            return RestResponse.Error(ApiGetSoknadErrorCode.FANT_IKKE_DOKUMENT)
        }

        return RestResponse.OK(dokument.tilSøknad())
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
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_DOKUMENT("Fant ikke søknadsdokument", HttpStatusCode.NotFound),
}
