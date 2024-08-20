package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsgodkjenningCommand(
    private val behandlingId: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID?,
    private val utbetaling: Utbetaling?,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val godkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime,
    private val ident: String,
    private val epostadresse: String,
    private val årsak: String?,
    private val begrunnelser: List<String>?,
    private val kommentar: String?,
    private val saksbehandleroverstyringer: List<UUID>,
    private val godkjenningsbehovhendelseId: UUID,
    private val saksbehandler: Saksbehandlerløsning.Saksbehandler,
    private val beslutter: Saksbehandlerløsning.Saksbehandler?,
    private val meldingDao: MeldingDao,
    private val godkjenningMediator: GodkjenningMediator,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingsgodkjenningCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val behovJson = meldingDao.finnUtbetalingsgodkjenningbehovJson(godkjenningsbehovhendelseId)
        val behov = UtbetalingsgodkjenningMessage(behovJson, utbetaling)
        if (godkjent) {
            godkjenningMediator.saksbehandlerUtbetaling(
                behandlingId = behandlingId,
                context = context,
                behov = behov,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                saksbehandlerIdent = ident,
                saksbehandlerEpost = epostadresse,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                godkjenttidspunkt = godkjenttidspunkt,
                saksbehandleroverstyringer = saksbehandleroverstyringer,
                sykefraværstilfelle = sykefraværstilfelle,
                spleisBehandlingId = spleisBehandlingId,
            )
        } else {
            godkjenningMediator.saksbehandlerAvvisning(
                behandlingId = behandlingId,
                context = context,
                behov = behov,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                saksbehandlerIdent = ident,
                saksbehandlerEpost = epostadresse,
                saksbehandler = saksbehandler,
                godkjenttidspunkt = godkjenttidspunkt,
                årsak = årsak,
                begrunnelser = begrunnelser,
                kommentar = kommentar,
                saksbehandleroverstyringer = saksbehandleroverstyringer,
                spleisBehandlingId = spleisBehandlingId,
            )
        }
        log.info("sender svar på godkjenningsbehov")
        return true
    }
}
