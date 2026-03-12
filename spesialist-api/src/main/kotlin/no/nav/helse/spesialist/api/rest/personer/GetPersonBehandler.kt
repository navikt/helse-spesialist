package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPerson
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Personinfo

class GetPersonBehandler(
    private val personinfoHenter: PersoninfoHenter,
    private val alleIdenterHenter: AlleIdenterHenter,
) : GetBehandler<Personer.PersonPseudoId, ApiPerson, ApiGetPersonErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPerson, ApiGetPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val identitetsnummer = person.id
            val personinfo =
                personinfoHenter.hentPersoninfo(identitetsnummer)
                    ?: return@medPerson RestResponse.Error(ApiGetPersonErrorCode.PERSON_IKKE_FUNNET)

            loggInfo("Hentet personinfo")

            val alleIdenter = alleIdenterHenter.hentAlleIdenter(identitetsnummer)

            val aktørId =
                alleIdenter
                    .asSequence()
                    .filter(AlleIdenterHenter.Ident::gjeldende)
                    .find { it.type == AlleIdenterHenter.IdentType.AKTORID }
                    ?.ident
                    ?: return@medPerson RestResponse.Error(ApiGetPersonErrorCode.AKTØRID_IKKE_FUNNET)

            val andreIdentitetsnumre =
                alleIdenter
                    .asSequence()
                    .filter { it.type == AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT }
                    .map { it.ident }
                    .filterNot { it == identitetsnummer.value }
                    .distinct()
                    .sorted()
                    .toList()

            loggInfo("Hentet identer")

            RestResponse.OK(
                ApiPerson(
                    identitetsnummer = identitetsnummer.value,
                    andreIdentitetsnumre = andreIdentitetsnumre,
                    aktørId = aktørId,
                    fornavn = personinfo.fornavn,
                    mellomnavn = personinfo.mellomnavn,
                    etternavn = personinfo.etternavn,
                    fødselsdato = personinfo.fødselsdato!!,
                    dødsdato = personinfo.dødsdato,
                    kjønn =
                        when (personinfo.kjønn) {
                            Personinfo.Kjønn.Kvinne -> ApiPerson.Kjønn.KVINNE
                            Personinfo.Kjønn.Mann -> ApiPerson.Kjønn.MANN
                            Personinfo.Kjønn.Ukjent, null -> ApiPerson.Kjønn.UKJENT
                        },
                    adressebeskyttelse =
                        when (personinfo.adressebeskyttelse) {
                            Personinfo.Adressebeskyttelse.Ugradert -> ApiPerson.Adressebeskyttelse.UGRADERT
                            Personinfo.Adressebeskyttelse.Fortrolig -> ApiPerson.Adressebeskyttelse.FORTROLIG
                            Personinfo.Adressebeskyttelse.StrengtFortrolig -> ApiPerson.Adressebeskyttelse.STRENGT_FORTROLIG
                            Personinfo.Adressebeskyttelse.StrengtFortroligUtland -> ApiPerson.Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND
                            Personinfo.Adressebeskyttelse.Ukjent -> ApiPerson.Adressebeskyttelse.UKJENT
                        },
                ),
            )
        }

    override val tag = Tags.PERSONER
}

enum class ApiGetPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    PERSON_IKKE_FUNNET("Fant ikke data for person", HttpStatusCode.NotFound),
    AKTØRID_IKKE_FUNNET("Fant ikke aktørId for person", HttpStatusCode.InternalServerError),
}
