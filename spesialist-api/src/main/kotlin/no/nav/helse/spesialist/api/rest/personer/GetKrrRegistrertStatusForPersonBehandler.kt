package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKrrRegistrertStatus
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetKrrRegistrertStatusForPersonBehandler(
    private val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
) : GetBehandler<Personer.PersonPseudoId.KrrRegistrertStatus, ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.KrrRegistrertStatus,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetKrrRegistrertStatusForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetKrrRegistrertStatusForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(person) }

    private fun behandleForPerson(person: Person): RestResponse<ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode> {
        val registrertStatus =
            try {
                krrRegistrertStatusHenter.hentForPerson(person.id.value)
            } catch (e: Exception) {
                loggThrowable("Feil ved kall til KRR", e)
                return RestResponse.Error(ApiGetKrrRegistrertStatusForPersonErrorCode.FEIL_VED_VIDERE_KALL)
            }

        val apiRegistrertStatus =
            when (registrertStatus) {
                KrrRegistrertStatusHenter.KrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING -> ApiKrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
                KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING -> ApiKrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
                KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_REGISTRERT_I_KRR -> ApiKrrRegistrertStatus.IKKE_REGISTRERT_I_KRR
            }

        loggInfo("Hentet KRR-status for person", "$apiRegistrertStatus")

        return RestResponse.OK(apiRegistrertStatus)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("KRR")
        }
    }
}

enum class ApiGetKrrRegistrertStatusForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FEIL_VED_VIDERE_KALL(
        "Klarte ikke hente status fra Kontakt- og Reservasjonsregisteret",
        HttpStatusCode.InternalServerError,
    ),
}
