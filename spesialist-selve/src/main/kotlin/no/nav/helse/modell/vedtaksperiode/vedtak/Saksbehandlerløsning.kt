package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.UtbetalingsgodkjenningCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.UtbetalingDao

/**
 * Behandler input til godkjenningsbehov fra saksbehandler som har blitt lagt på rapid-en av API-biten av spesialist.
 */
internal class Saksbehandlerløsning(
    override val id: UUID,
    behandlingId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    godkjent: Boolean,
    saksbehandlerIdent: String,
    epostadresse: String,
    godkjenttidspunkt: LocalDateTime,
    årsak: String?,
    begrunnelser: List<String>?,
    kommentar: String?,
    saksbehandleroverstyringer: List<UUID>,
    private val oppgaveId: Long,
    godkjenningsbehovhendelseId: UUID,
    hendelseDao: HendelseDao,
    private val oppgaveDao: OppgaveDao,
    godkjenningMediator: GodkjenningMediator,
    utbetalingDao: UtbetalingDao,
    sykefraværstilfelle: Sykefraværstilfelle,
) : Kommandohendelse, MacroCommand() {
    private val utbetaling = utbetalingDao.utbetalingFor(oppgaveId)

    override val commands = listOf(
        UtbetalingsgodkjenningCommand(
            behandlingId = behandlingId,
            hendelseId = id,
            godkjent = godkjent,
            saksbehandlerIdent = saksbehandlerIdent,
            epostadresse = epostadresse,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            godkjenningsbehovhendelseId = godkjenningsbehovhendelseId,
            hendelseDao = hendelseDao,
            godkjenningMediator = godkjenningMediator,
            vedtaksperiodeId = vedtaksperiodeId(),
            fødselsnummer = fødselsnummer,
            utbetaling = utbetaling,
            sykefraværstilfelle = sykefraværstilfelle
        ),
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
    override fun toJson() = json

}
