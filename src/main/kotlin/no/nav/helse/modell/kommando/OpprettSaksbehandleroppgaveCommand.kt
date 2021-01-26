package no.nav.helse.modell.kommando

import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import org.slf4j.LoggerFactory
import java.util.*

internal class OpprettSaksbehandleroppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveMediator: OppgaveMediator,
    private val automatisering: Automatisering,
    private val hendelseId: UUID,
    private val egenAnsattDao: EgenAnsattDao,
    private val personDao: PersonDao,
    private val risikovurderingDao: RisikovurderingDao
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (automatisering.harBlittAutomatiskBehandlet(vedtaksperiodeId, hendelseId)) return true
        if (erEgenAnsatt) return true
        if (tilhørerUtlandsenhet) return true

        val oppgave = when {
            automatisering.erStikkprøve(vedtaksperiodeId, hendelseId) -> Oppgave.stikkprøve(vedtaksperiodeId)
            risikovurderingDao.kreverSupersaksbehandler(vedtaksperiodeId) -> Oppgave.riskQA(vedtaksperiodeId)
            else -> Oppgave.søknad(vedtaksperiodeId)
        }
        logg.info("Oppretter saksbehandleroppgave")
        oppgaveMediator.opprett(oppgave)
        return true
    }

    private val erEgenAnsatt get() = egenAnsattDao.erEgenAnsatt(fødselsnummer) ?: false
    private val tilhørerUtlandsenhet get() = personDao.tilhørerUtlandsenhet(fødselsnummer)
}
