package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class LøsGodkjenningsbehov(
    private val utbetaling: Utbetaling,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val godkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime,
    private val ident: String,
    private val epostadresse: String,
    private val årsak: String?,
    private val begrunnelser: List<String>?,
    private val kommentar: String?,
    private val saksbehandleroverstyringer: List<UUID>,
    private val saksbehandler: Saksbehandlerløsning.Saksbehandler,
    private val beslutter: Saksbehandlerløsning.Saksbehandler?,
    private val godkjenningMediator: GodkjenningMediator,
    private val godkjenningsbehovData: GodkjenningsbehovData,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(LøsGodkjenningsbehov::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (godkjent) {
            godkjenningMediator.saksbehandlerUtbetaling(
                context = context,
                behov = godkjenningsbehovData,
                utbetaling = utbetaling,
                saksbehandlerIdent = ident,
                saksbehandlerEpost = epostadresse,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                godkjenttidspunkt = godkjenttidspunkt,
                saksbehandleroverstyringer = saksbehandleroverstyringer,
                sykefraværstilfelle = sykefraværstilfelle,
            )
        } else {
            godkjenningMediator.saksbehandlerAvvisning(
                context = context,
                behov = godkjenningsbehovData,
                utbetaling = utbetaling,
                saksbehandlerIdent = ident,
                saksbehandlerEpost = epostadresse,
                saksbehandler = saksbehandler,
                godkjenttidspunkt = godkjenttidspunkt,
                årsak = årsak,
                begrunnelser = begrunnelser,
                kommentar = kommentar,
                saksbehandleroverstyringer = saksbehandleroverstyringer,
            )
        }
        logg.info("sender svar på godkjenningsbehov")
        return true
    }
}
