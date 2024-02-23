package no.nav.helse.modell.kommando

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import org.slf4j.LoggerFactory

internal class UtbetalingsgodkjenningCommand(
    private val id: UUID,
    private val behandlingId: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
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
    private val hendelseDao: HendelseDao,
    private val godkjenningMediator: GodkjenningMediator
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingsgodkjenningCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val behovJson = hendelseDao.finnUtbetalingsgodkjenningbehovJson(godkjenningsbehovhendelseId)
        val behov = UtbetalingsgodkjenningMessage(behovJson, utbetaling)
        if (godkjent) {
            godkjenningMediator.saksbehandlerUtbetaling(
                behandlingId = behandlingId,
                hendelseId = id,
                context = context,
                behov = behov,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                saksbehandlerIdent = ident,
                saksbehandlerEpost = epostadresse,
                godkjenttidspunkt = godkjenttidspunkt,
                saksbehandleroverstyringer = saksbehandleroverstyringer,
                sykefraværstilfelle = sykefraværstilfelle
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
                godkjenttidspunkt = godkjenttidspunkt,
                årsak = årsak,
                begrunnelser = begrunnelser,
                kommentar = kommentar,
                saksbehandleroverstyringer = saksbehandleroverstyringer
            )
        }
        log.info("sender svar på godkjenningsbehov")
        return true
    }
}

