package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.tilUtgåendeHendelse
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer

internal class AvbrytOppgaveCommand(
    private val identitetsnummer: Identitetsnummer,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val oppgave = sessionContext.oppgaveRepository.finnGjeldendeForPerson(identitetsnummer)

        if (oppgave == null) {
            loggInfo("Fant ingen oppgave for person, ingen oppgave å avbryte", "fødselsnummer" to identitetsnummer.value)
            return true
        }

        oppgave.avbryt()
        sessionContext.oppgaveRepository.lagre(oppgave)
        oppgave.konsumerHendelser().forEach {
            outbox.leggTil(identitetsnummer, it.tilUtgåendeHendelse(), "avbrutt oppgave")
        }
        return true
    }
}
