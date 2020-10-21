package no.nav.helse.modell.kommando

import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.egenAnsatt.EgenAnsattDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import org.slf4j.LoggerFactory
import java.util.*

internal class OpprettSaksbehandleroppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val reservasjonDao: ReservasjonDao,
    private val oppgaveMediator: OppgaveMediator,
    private val automatisering: Automatisering,
    private val hendelseId: UUID,
    egenAnsattDao: EgenAnsattDao
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgaveCommand::class.java)
    }

    private val oppgave = Oppgave.avventerSaksbehandler(this::class.java.simpleName, vedtaksperiodeId)

    override fun execute(context: CommandContext): Boolean {
        if (automatisering.harBlittAutomatiskBehandlet(vedtaksperiodeId, hendelseId)) return true
        if (erEgenAnsatt) return true

        logg.info("Oppretter saksbehandleroppgave")
        reservasjonDao.hentReservasjonFor(fødselsnummer)?.let { reservasjon ->
            oppgaveMediator.tildel(oppgave, reservasjon.first, reservasjon.second)
        } ?: oppgaveMediator.nyOppgave(oppgave)

        return true
    }

    private val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer) ?: false
}
