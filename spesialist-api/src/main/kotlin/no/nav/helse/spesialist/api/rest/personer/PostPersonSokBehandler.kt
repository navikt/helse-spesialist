package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.PersoninfoKlargjører
import no.nav.helse.spesialist.application.PersoninfoKlargjører.KlargjøringResultat
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostPersonSokBehandler(
    private val alleIdenterHenter: AlleIdenterHenter,
    personinfoHenter: PersoninfoHenter,
) : PostBehandler<Personer.Sok, ApiPersonSokRequest, ApiPersonSokResponse, ApiPostPersonSokErrorCode> {
    private val personinfoKlargjører = PersoninfoKlargjører(personinfoHenter)

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
                        ?: when (val resultat = opprettPerson(identitet)) {
                            is Either.Failure -> return RestResponse.Error(resultat.error)
                            is Either.Success -> resultat.result
                        }.also {
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
            when (personinfoKlargjører.klargjør(person)) {
                is KlargjøringResultat.IkkeFunnet -> return@medPerson RestResponse.Error(ApiPostPersonSokErrorCode.PERSONINFO_IKKE_FUNNET_I_PDL)
                is KlargjøringResultat.OppslagFeilet -> return@medPerson RestResponse.Error(ApiPostPersonSokErrorCode.PERSONINFO_OPPSLAG_FEILET)
                KlargjøringResultat.Klargjort -> Unit
            }
            kallKontekst.transaksjon.personRepository.lagre(person)

            val personPseudoId = kallKontekst.personPseudoIdProvider.nyPersonPseudoId(person.id)
            val body = ApiPersonSokResponse(personPseudoId = personPseudoId.value)

            RestResponse.OK(body)
        }
    }

    private fun opprettPerson(identitetsnummer: Identitetsnummer): Either<Person, ApiPostPersonSokErrorCode> {
        val aktørId =
            alleIdenterHenter
                .hentAlleIdenter(identitetsnummer)
                .filter { it.gjeldende }
                .find { it.type == AlleIdenterHenter.IdentType.AKTORID }
                ?.ident
                ?: return Either.Failure(ApiPostPersonSokErrorCode.AKTØRID_IKKE_FUNNET_I_PDL)
        return Either.Success(
            Person.Factory.ny(
                identitetsnummer,
                aktørId,
                null,
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
    AKTØRID_IKKE_FUNNET_I_PDL("AktørId for personen fins ikke i PDL", HttpStatusCode.NotFound),
    PERSONINFO_IKKE_FUNNET_I_PDL("Personinfo for personen fins ikke i PDL", HttpStatusCode.NotFound),
    PERSONINFO_OPPSLAG_FEILET("Klarte ikke å hente personinfo", HttpStatusCode.BadGateway),
}
