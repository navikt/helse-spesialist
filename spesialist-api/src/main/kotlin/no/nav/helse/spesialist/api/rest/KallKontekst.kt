package no.nav.helse.spesialist.api.rest

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.withMDC
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class KallKontekst(
    val saksbehandler: Saksbehandler,
    val tilgangsgrupper: Set<Tilgangsgruppe>,
    val brukerroller: Set<Brukerrolle>,
    val transaksjon: SessionContext,
    val outbox: Outbox,
) {
    fun <R, E : ApiErrorCode> medPerson(
        personPseudoIdResource: Personer.PersonPseudoId,
        personIkkeFunnetErrorCode: E,
        manglerTilgangTilPersonErrorCode: E,
        block: (person: Person) -> RestResponse<R, E>,
    ): RestResponse<R, E> {
        val identitetsnummer =
            transaksjon.personPseudoIdDao.hentIdentitetsnummer(PersonPseudoId.fraString(personPseudoIdResource.pseudoId))
                ?: return RestResponse.Error(personIkkeFunnetErrorCode)

        return withMDC(mapOf("identitetsnummer" to identitetsnummer.value)) {
            val person = transaksjon.personRepository.finn(identitetsnummer)

            if (person == null) {
                teamLogs.warn("Person med identitetsnummer $identitetsnummer ble ikke funnet")
                return@withMDC RestResponse.Error(personIkkeFunnetErrorCode)
            }

            if (!person.kanSeesAvSaksbehandlerMedGrupper(brukerroller)) {
                teamLogs.warn("Saksbehandler har ikke tilgang til person med identitetsnummer $identitetsnummer")
                return@withMDC RestResponse.Error(manglerTilgangTilPersonErrorCode)
            }

            val oppgaveId = transaksjon.oppgaveDao.finnOppgaveId(identitetsnummer.value)

            // Best effort for å finne ut om saksbehandler har tilgang til oppgaven som gjelder
            // Litt vanskelig å få pent så lenge vi har dynamisk resolving av resten, og tilsynelatende "mange" oppgaver
            val harTilgangTilOppgave =
                oppgaveId?.let { oppgaveId ->
                    transaksjon.oppgaveRepository
                        .finn(oppgaveId)
                        ?.kanSeesAv(brukerroller, tilgangsgrupper)
                } ?: true

            if (!harTilgangTilOppgave) {
                loggWarn("Saksbehandler mangler tilgang til aktiv oppgave på denne personen")
                return@withMDC RestResponse.Error(manglerTilgangTilPersonErrorCode)
            }

            block(person)
        }
    }
}
