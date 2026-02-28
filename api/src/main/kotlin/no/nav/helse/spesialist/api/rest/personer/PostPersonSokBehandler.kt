package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPersonSokRequest
import no.nav.helse.spesialist.api.rest.ApiPersonSokResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostPersonSokBehandler : PostBehandler<Personer.Sok, ApiPersonSokRequest, ApiPersonSokResponse, ApiPostPersonSokErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.Sok,
        request: ApiPersonSokRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPersonSokResponse, ApiPostPersonSokErrorCode> {
        val aktørId = request.aktørId
        val identitetsnummer = request.identitetsnummer
        val person =
            when {
                aktørId != null && identitetsnummer != null -> {
                    return RestResponse.Error(ApiPostPersonSokErrorCode.FOR_MANGE_INPUTPARAMETERE)
                }

                aktørId != null -> {
                    loggInfo("Søker etter person med aktørId", "aktørId" to aktørId)
                    kallKontekst.transaksjon.personRepository
                        .finnAlleMedAktørId(aktørId)
                        .firstOrNull()
                }

                identitetsnummer != null -> {
                    loggInfo("Søker etter person med identitetsnummer", "identitetsnummer" to identitetsnummer)
                    kallKontekst.transaksjon.personRepository.finn(Identitetsnummer.fraString(identitetsnummer))
                }

                else -> {
                    return RestResponse.Error(ApiPostPersonSokErrorCode.MANGLER_INPUTPARAMETERE)
                }
            }
        if (person == null) {
            return RestResponse.Error(ApiPostPersonSokErrorCode.PERSON_IKKE_FUNNET)
        }

        return kallKontekst.medMdcOgAttribute(MdcKey.IDENTITETSNUMMER to person.id.value) {
            val personPseudoId = kallKontekst.transaksjon.personPseudoIdDao.nyPersonPseudoId(person.id)
            kallKontekst.medMdcOgAttribute(MdcKey.PERSON_PSEUDO_ID to personPseudoId.value.toString()) {
                val klarForVisning = person.harDataNødvendigForVisning()
                if (!klarForVisning) {
                    if (!kallKontekst.transaksjon.personKlargjoresDao.klargjøringPågår(person.id.value)) {
                        kallKontekst.outbox.leggTil(person.id, KlargjørPersonForVisning, "klargjørPersonForVisning")
                        kallKontekst.transaksjon.personKlargjoresDao.personKlargjøres(person.id.value)
                    }
                }

                val body = ApiPersonSokResponse(personPseudoId = personPseudoId.value, klarForVisning = klarForVisning)

                loggInfo("Fant person ved søk")

                RestResponse.OK(body)
            }
        }
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
