package no.nav.helse.spesialist.api.rest.personer.tildeling

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.DeleteBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.oppgaver.leggTilIOutbox
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo

class DeleteTildelingBehandler : DeleteBehandler<Personer.PersonPseudoId.Tildeling, Unit, ApiDeleteTildelingErrorCode> {
    override val tag: Tags = Tags.OPPGAVER

    override fun behandle(
        resource: Personer.PersonPseudoId.Tildeling,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiDeleteTildelingErrorCode> {
        return kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiDeleteTildelingErrorCode.OPPGAVE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiDeleteTildelingErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) {
            val oppgave = kallKontekst.transaksjon.oppgaveRepository.finnAktivForPerson(it.id) ?: error("Fant ikke oppgave")
            if (!oppgave.erTildelt()) {
                loggInfo("Oppgaven er ikke tildelt, gjør ikke noe videre")
                return@medPerson RestResponse.NoContent()
            }
            oppgave.forsøkAvmelding(saksbehandler = kallKontekst.saksbehandler)
            kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            oppgave.konsumerHendelser().leggTilIOutbox(identitetsnummer = it.id, kallKontekst = kallKontekst, "Oppgave avmeldt")
            RestResponse.NoContent()
        }
    }
}

enum class ApiDeleteTildelingErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    OPPGAVE_IKKE_FUNNET("Oppgave ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
