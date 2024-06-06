package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AutomatiseringForEksisterendeOppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val oppgaveService: OppgaveService,
    private val utbetaling: Utbetaling,
    private val periodetype: Periodetype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val spleisBehandlingId: UUID?,
    private val organisasjonsnummer: String,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringForEksisterendeOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(
            fødselsnummer,
            vedtaksperiodeId,
            hendelseId,
            utbetaling,
            periodetype,
            sykefraværstilfelle,
            organisasjonsnummer,
        ) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
            godkjenningMediator.automatiskUtbetaling(
                context = context,
                behov = behov,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                hendelseId = hendelseId,
                spleisBehandlingId = spleisBehandlingId,
            )
            logg.info("Oppgave avbrytes for vedtaksperiode $vedtaksperiodeId på grunn av automatisering")
            oppgaveService.avbrytOppgaveFor(vedtaksperiodeId)
        }
        return true
    }
}
