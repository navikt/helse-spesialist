package no.nav.helse.mediator.oppgave

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype

class Oppgavehenter(
    private val repository: OppgaveRepository,
) {
    fun oppgave(id: Long): Oppgave {
        val oppgave = repository.finnOppgave(id)
            ?: throw IllegalStateException("Forventer å finne oppgave med oppgaveId=$id")

        return Oppgave(
            id = oppgave.id,
            type = enumValueOf<Oppgavetype>(oppgave.type),
            status = enumValueOf<Oppgavestatus>(oppgave.status),
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            tildelt = oppgave.tildelt?.let {
                Saksbehandler(it.epostadresse, it.oid, it.navn, it.ident)
            },
            påVent = oppgave.påVent
        )
    }
}