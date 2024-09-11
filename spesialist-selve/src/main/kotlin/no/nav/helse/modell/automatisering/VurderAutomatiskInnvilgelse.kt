package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import org.slf4j.LoggerFactory

internal class VurderAutomatiskInnvilgelse(
    private val automatisering: Automatisering,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val godkjenningsbehov: GodkjenningsbehovData,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskInnvilgelse::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(
            fødselsnummer = godkjenningsbehov.fødselsnummer,
            vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId,
            hendelseId = godkjenningsbehov.id,
            utbetaling = utbetaling,
            periodetype = godkjenningsbehov.periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
            organisasjonsnummer = godkjenningsbehov.organisasjonsnummer,
        ) {
            godkjenningMediator.automatiskUtbetaling(
                context = context,
                behov = godkjenningsbehov,
                utbetaling = utbetaling,
            )
            logg.info("Automatisk godkjenning for vedtaksperiode ${godkjenningsbehov.vedtaksperiodeId}")
            ferdigstill(context)
        }

        return true
    }
}
