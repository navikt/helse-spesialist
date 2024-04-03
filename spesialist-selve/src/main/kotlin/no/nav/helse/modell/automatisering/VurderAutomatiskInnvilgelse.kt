package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderAutomatiskInnvilgelse(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID?,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val periodetype: Periodetype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskInnvilgelse::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetaling, periodetype, sykefraværstilfelle) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
            godkjenningMediator.automatiskUtbetaling(
                context = context,
                behov = behov,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                hendelseId = hendelseId,
                spleisBehandlingId = spleisBehandlingId,
            )
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
            ferdigstill(context)
        }

        return true
    }
}
