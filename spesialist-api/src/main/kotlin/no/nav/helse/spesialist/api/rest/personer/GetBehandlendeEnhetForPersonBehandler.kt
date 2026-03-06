package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiBehandlendeEnhet
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.BehandlendeEnhetHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetBehandlendeEnhetForPersonBehandler(
    private val behandlendeEnhetHenter: BehandlendeEnhetHenter,
) : GetBehandler<Personer.PersonPseudoId.BehandlendeEnhet, ApiBehandlendeEnhet, ApiGetBehandlendeEnhetForPersonErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.BehandlendeEnhet,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiBehandlendeEnhet, ApiGetBehandlendeEnhetForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetBehandlendeEnhetForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetBehandlendeEnhetForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(person) }

    private fun behandleForPerson(person: Person): RestResponse<ApiBehandlendeEnhet, ApiGetBehandlendeEnhetForPersonErrorCode> {
        val enhet = behandlendeEnhetHenter.hentFor(person.id)

        if (enhet == null) {
            loggWarn("Fant ingen behandlende enhet for person")
            return RestResponse.Error(ApiGetBehandlendeEnhetForPersonErrorCode.BEHANDLENDE_ENHET_IKKE_FUNNET)
        }

        loggInfo("Hentet behandlende enhet for person", "enhet" to enhet)

        return RestResponse.OK(ApiBehandlendeEnhet(enhetNr = enhet.enhetNr, navn = enhet.navn, type = enhet.type))
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("NORG")
        }
    }
}

enum class ApiGetBehandlendeEnhetForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    BEHANDLENDE_ENHET_IKKE_FUNNET("Fant ingen behandlende enhet for personen", HttpStatusCode.NotFound),
}
