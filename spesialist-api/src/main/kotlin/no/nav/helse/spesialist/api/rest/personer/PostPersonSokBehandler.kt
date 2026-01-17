package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPersonSokRequest
import no.nav.helse.spesialist.api.rest.ApiPersonSokResponse
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostPersonSokBehandler : PostBehandler<Personer.Sok, ApiPersonSokRequest, ApiPersonSokResponse, ApiPostPersonSokErrorCode> {
    override fun behandle(
        resource: Personer.Sok,
        request: ApiPersonSokRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<ApiPersonSokResponse, ApiPostPersonSokErrorCode> {
        val aktørId = request.aktørId
        val identitetsnummer = request.identitetsnummer
        val person =
            when {
                aktørId != null && identitetsnummer != null ->
                    return RestResponse.Error(ApiPostPersonSokErrorCode.FOR_MANGE_INPUTPARAMETERE)

                aktørId != null ->
                    transaksjon.personRepository.finnAlleMedAktørId(aktørId).firstOrNull()

                identitetsnummer != null ->
                    transaksjon.personRepository.finn(Identitetsnummer.fraString(identitetsnummer))

                else ->
                    return RestResponse.Error(ApiPostPersonSokErrorCode.MANGLER_INPUTPARAMETERE)
            }
        if (person == null) {
            return RestResponse.Error(ApiPostPersonSokErrorCode.PERSON_IKKE_FUNNET)
        }

        val personPseudoId = transaksjon.personPseudoIdDao.nyPersonPseudoId(identitetsnummer = person.id)

        val klarForVisning = person.harDataNødvendigForVisning()
        if (!klarForVisning) {
            val fødselsnummer = person.id.value
            if (!transaksjon.personKlargjoresDao.klargjøringPågår(fødselsnummer)) {
                outbox.leggTil(person.id.value, KlargjørPersonForVisning, "klargjørPersonForVisning")
                transaksjon.personKlargjoresDao.personKlargjøres(person.id.value)
            }
        }

        return RestResponse.OK(
            ApiPersonSokResponse(
                personPseudoId = personPseudoId.value,
                klarForVisning = klarForVisning,
            ),
        )
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Personsøk")
        }
    }
}

enum class ApiPostPersonSokErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    FOR_MANGE_INPUTPARAMETERE(
        "Enten aktørId eller identitetsnummer må spesifiseres, ikke begge",
        HttpStatusCode.BadRequest,
    ),
    MANGLER_INPUTPARAMETERE(
        "Enten aktørId eller identitetsnummer må spesifiseres, begge manglet",
        HttpStatusCode.BadRequest,
    ),
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
}
