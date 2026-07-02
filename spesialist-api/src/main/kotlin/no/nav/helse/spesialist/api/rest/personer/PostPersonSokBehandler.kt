package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPersonSokRequest
import no.nav.helse.spesialist.api.rest.ApiPersonSokResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostPersonSokBehandler(
    private val alleIdenterHenter: AlleIdenterHenter,
    private val personinfoHenter: PersoninfoHenter,
) : PostBehandler<Personer.Sok, ApiPersonSokRequest, ApiPersonSokResponse, ApiPostPersonSokErrorCode> {
    override val påkrevdTilgang = Tilgang.Les
    override val tag = Tags.PERSONER

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
                        ?: return RestResponse.Error(ApiPostPersonSokErrorCode.PERSON_IKKE_FUNNET)
                }

                identitetsnummer != null -> {
                    loggInfo("Søker etter person med identitetsnummer", "identitetsnummer" to identitetsnummer)

                    val identitet = Identitetsnummer.fraString(identitetsnummer)
                    kallKontekst.transaksjon.personRepository.finn(identitet)
                        ?: opprettPerson(identitet)
                            .getOrElse { return RestResponse.Error(ApiPostPersonSokErrorCode.AKTØRID_IKKE_FUNNET) }
                            .also {
                                loggInfo("Kjenner ikke til personen fra før av, lagrer personen")
                                kallKontekst.transaksjon.personRepository.lagre(it)
                            }
                }

                else -> {
                    return RestResponse.Error(ApiPostPersonSokErrorCode.MANGLER_INPUTPARAMETERE)
                }
            }
        return kallKontekst.medPerson(
            person.id,
            personIkkeFunnet = { ApiPostPersonSokErrorCode.PERSON_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostPersonSokErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) {
            val personPseudoId = kallKontekst.personPseudoIdProvider.nyPersonPseudoId(person.id)
            val klarForVisning = person.harDataNødvendigForVisning()
            if (!klarForVisning) {
                if (!kallKontekst.transaksjon.personKlargjoresDao.klargjøringPågår(person.id.value)) {
                    kallKontekst.outbox.leggTil(person.id, KlargjørPersonForVisning, "klargjørPersonForVisning")
                    kallKontekst.transaksjon.personKlargjoresDao.personKlargjøres(person.id.value)
                }
            }

            val body = ApiPersonSokResponse(personPseudoId = personPseudoId.value, klarForVisning = klarForVisning)

            RestResponse.OK(body)
        }
    }

    private fun opprettPerson(identitetsnummer: Identitetsnummer): Result<Person> {
        val aktørId =
            alleIdenterHenter
                .hentAlleIdenter(identitetsnummer)
                .filter { it.gjeldende }
                .find { it.type == AlleIdenterHenter.IdentType.AKTORID }
                ?.ident
                ?: return Result.failure(IllegalArgumentException("Fant ikke aktørId for identitetsnummer $identitetsnummer"))
        val personinfo = personinfoHenter.hentPersoninfo(identitetsnummer)
        return Result.success(
            Person.Factory.ny(
                identitetsnummer,
                aktørId,
                personinfo,
                null,
            ),
        )
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
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.NotFound),
    AKTØRID_IKKE_FUNNET("Fant ikke aktørId for person", HttpStatusCode.InternalServerError),
}
