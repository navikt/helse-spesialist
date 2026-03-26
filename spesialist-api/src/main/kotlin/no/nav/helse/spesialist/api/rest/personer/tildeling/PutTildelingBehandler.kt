package no.nav.helse.spesialist.api.rest.personer.tildeling

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiTildelingRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PutBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.oppgaver.leggTilIOutbox
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo

class PutTildelingBehandler : PutBehandler<Personer.PersonPseudoId.Tildeling, ApiTildelingRequest, Unit, ApiPostTildelingErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: Personer.PersonPseudoId.Tildeling,
        request: ApiTildelingRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostTildelingErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPostTildelingErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostTildelingErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) {
            val oppgave = kallKontekst.transaksjon.oppgaveRepository.finnAktivForPerson(it.id) ?: error("Fant ikke oppgave")

            if (!oppgave.kanTildelesTil(brukerroller = kallKontekst.brukerroller)) {
                return@medPerson RestResponse.Error(ApiPostTildelingErrorCode.MANGLER_TILGANG_TIL_OPPGAVE)
            }

            if (oppgave.erTildelt()) {
                if (oppgave.tildeltTil == kallKontekst.saksbehandler.id) {
                    loggInfo("Oppgaven er allerede tilmeldt saksbehandler. Gjør ikke noe videre")
                    return@medPerson RestResponse.NoContent()
                } else {
                    loggInfo("Oppgaven er allerede tildelt en annen saksbehandler", "tildeltSaksbehandler" to oppgave.tildeltTil?.value)
                    return@medPerson RestResponse.Error(ApiPostTildelingErrorCode.OPPGAVE_TILDELT_ANNEN_SAKSBEHANDLER)
                }
            }

            oppgave.tildelTil(kallKontekst.saksbehandler, brukerroller = kallKontekst.brukerroller)

            kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            oppgave.konsumerHendelser().leggTilIOutbox(identitetsnummer = it.id, kallKontekst = kallKontekst, "Oppgave tildelt")
            RestResponse.NoContent()
        }
}

enum class ApiPostTildelingErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("Personpseudoid ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    MANGLER_TILGANG_TIL_OPPGAVE("Mangler tilgang til oppgave", HttpStatusCode.Forbidden),
    OPPGAVE_TILDELT_ANNEN_SAKSBEHANDLER("Oppgaven er allerede tildelt en annen saksbehandler", HttpStatusCode.BadRequest),
}
