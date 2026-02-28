package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPerson
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.LocalDate
import java.time.Period

class GetPersonBehandler(
    private val personinfoHenter: PersoninfoHenter,
) : GetBehandler<Personer.PersonPseudoId, ApiPerson, ApiGetPersonErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPerson, ApiGetPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val personinfo =
                personinfoHenter.hentPersoninfo(person.id.value)
                    ?: return@medPerson RestResponse.Error(ApiGetPersonErrorCode.PERSON_IKKE_FUNNET)

            loggInfo("Hentet personopplysninger")

            RestResponse.OK(
                ApiPerson(
                    identitetsnummer = person.id.value,
                    andreIdentitetsnumre =
                        kallKontekst.transaksjon.personRepository
                            .finnAlleMedAktørId(person.aktørId)
                            .asSequence()
                            .map(Person::id)
                            .filterNot { it == person.id }
                            .map(Identitetsnummer::value)
                            .distinct()
                            .sorted()
                            .toList(),
                    aktørId = person.aktørId,
                    fornavn = personinfo.fornavn + personinfo.mellomnavn?.let { " $it" }.orEmpty(),
                    etternavn = personinfo.etternavn,
                    kjønn =
                        when (personinfo.kjønn) {
                            Personinfo.Kjønn.Kvinne -> ApiPerson.Kjønn.KVINNE
                            Personinfo.Kjønn.Mann -> ApiPerson.Kjønn.MANN
                            Personinfo.Kjønn.Ukjent, null -> ApiPerson.Kjønn.UKJENT
                        },
                    alder = Period.between(personinfo.fødselsdato!!, LocalDate.now()).years,
                    boenhet = ApiPerson.Boenhet(person.enhetRef!!.toString().padStart(4, '0')),
                ),
            )
        }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Person")
        }
    }
}

enum class ApiGetPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    PERSON_IKKE_FUNNET("Fant ikke data for person", HttpStatusCode.NotFound),
}
