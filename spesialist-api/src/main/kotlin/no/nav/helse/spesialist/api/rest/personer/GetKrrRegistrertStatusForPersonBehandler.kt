package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKrrRegistrertStatus
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggThrowable

class GetKrrRegistrertStatusForPersonBehandler(
    private val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
) : GetBehandler<Personer.PersonPseudoId.KrrRegistrertStatus, ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.KrrRegistrertStatus,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiKrrRegistrertStatus, ApiGetKrrRegistrertStatusForPersonErrorCode> {
        val personId = resource.parent.pseudoId
        val pseudoId = PersonPseudoId.fraString(personId)
        val identitetsnummer =
            kallKontekst.transaksjon.personPseudoIdDao.hentIdentitetsnummer(pseudoId)
                ?: return RestResponse.Error(ApiGetKrrRegistrertStatusForPersonErrorCode.PERSON_IKKE_FUNNET)

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = identitetsnummer,
                brukerroller = kallKontekst.brukerroller,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiGetKrrRegistrertStatusForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

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
