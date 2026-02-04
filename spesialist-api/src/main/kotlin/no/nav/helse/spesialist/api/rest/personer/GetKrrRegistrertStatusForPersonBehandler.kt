package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKrrRegistrertStatus
import no.nav.helse.spesialist.api.rest.GetForPersonBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetKrrRegistrertStatusForPersonBehandler(
    private val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
) : GetForPersonBehandler<Personer.PersonPseudoId.KrrRegistrertStatus, ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode>(
        personPseudoId = { resource -> resource.parent },
        personIkkeFunnet = ApiGetKrrRegistrertStatusForPersonErrorCode.PERSON_IKKE_FUNNET,
        manglerTilgangTilPerson = ApiGetKrrRegistrertStatusForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON,
    ) {
    override val påkrevdeTilganger: Set<Tilgang> = setOf(Tilgang.Les, Tilgang.Skriv)

    override fun behandle(
        resource: Personer.PersonPseudoId.KrrRegistrertStatus,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode> {
        val identitetsnummer = person.id
        val registrertStatus =
            try {
                runBlocking { krrRegistrertStatusHenter.hentForPerson(identitetsnummer.value) }
            } catch (e: Exception) {
                loggThrowable("Feil ved kall til KRR", e)
                return RestResponse.Error(ApiGetKrrRegistrertStatusForPersonErrorCode.FEIL_VED_VIDERE_KALL)
            }

        return RestResponse.OK(
            when (registrertStatus) {
                KrrRegistrertStatusHenter.KrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING -> ApiKrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
                KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING -> ApiKrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
                KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_REGISTRERT_I_KRR -> ApiKrrRegistrertStatus.IKKE_REGISTRERT_I_KRR
            },
        )
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
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FEIL_VED_VIDERE_KALL(
        "Klarte ikke hente status fra Kontakt- og Reservasjonsregisteret",
        HttpStatusCode.InternalServerError,
    ),
}
