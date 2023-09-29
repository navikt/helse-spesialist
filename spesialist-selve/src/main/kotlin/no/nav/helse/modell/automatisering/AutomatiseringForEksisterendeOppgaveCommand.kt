package no.nav.helse.modell.automatisering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.slf4j.LoggerFactory

internal class AutomatiseringForEksisterendeOppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val oppgaveMediator: OppgaveMediator,
    private val utbetaling: Utbetaling,
    private val periodetype: Periodetype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val periodeTom: LocalDate
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringForEksisterendeOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetaling, periodetype, sykefraværstilfelle, periodeTom) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
            godkjenningMediator.automatiskUtbetaling(context, behov, vedtaksperiodeId, fødselsnummer, hendelseId)
            logg.info("Oppgave avbrytes for vedtaksperiode $vedtaksperiodeId på grunn av automatisering")
            oppgaveMediator.avbrytOppgaveFor(vedtaksperiodeId)
        }
        return true
    }
}
