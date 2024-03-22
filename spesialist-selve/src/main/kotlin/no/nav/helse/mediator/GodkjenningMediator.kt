package no.nav.helse.mediator

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.automatiseringsteller
import no.nav.helse.automatiskAvvistÅrsakerTeller
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingPayload
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingUtfall
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.slf4j.LoggerFactory

internal class GodkjenningMediator(
    private val vedtakDao: VedtakDao,
    private val opptegnelseDao: OpptegnelseDao,
    val oppgaveDao: OppgaveDao,
    val utbetalingDao: UtbetalingDao,
    val meldingDao: MeldingDao,
) {
    internal fun saksbehandlerUtbetaling(
        behandlingId: UUID,
        hendelseId: UUID,
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        beslutter: Saksbehandlerløsning.Saksbehandler?,
        godkjenttidspunkt: LocalDateTime,
        saksbehandleroverstyringer: List<UUID>,
        sykefraværstilfelle: Sykefraværstilfelle,
    ) {
        behov.godkjennManuelt(
            behandlingId = behandlingId,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            saksbehandleroverstyringer = saksbehandleroverstyringer
        )
        sykefraværstilfelle.håndterGodkjent(saksbehandlerIdent, vedtaksperiodeId, hendelseId)

        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeGodkjentManuelt(vedtaksperiodeId, fødselsnummer, saksbehandler, beslutter, vedtakDao).toJson())
    }

    internal fun saksbehandlerAvvisning(
        behandlingId: UUID,
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
    ) {
        behov.avvisManuelt(
            behandlingId = behandlingId,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer
        )
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeAvvistManuelt(vedtaksperiodeId, fødselsnummer, saksbehandler, vedtakDao).toJson())
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        hendelseId: UUID,
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeGodkjentAutomatisk(vedtaksperiodeId, fødselsnummer, vedtakDao).toJson())
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer,
            AutomatiskBehandlingPayload(hendelseId, AutomatiskBehandlingUtfall.UTBETALT),
            OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV
        )
        automatiseringsteller.inc()
        sikkerLogg.info(
            "Automatisk godkjenning av vedtaksperiode $vedtaksperiodeId for {}",
            keyValue("fødselsnummer", fødselsnummer)
        )
    }

    internal fun automatiskAvvisning(publiserer: Publiserer, begrunnelser: List<String>, oppgaveId: Long) {
        val utbetaling = utbetalingDao.utbetalingFor(oppgaveId) ?: return
        val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
        val hendelseId = oppgaveDao.finnGodkjenningsbehov(fødselsnummer)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer)
        automatiskAvvisning(publiserer, vedtaksperiodeId, begrunnelser, utbetaling, hendelseId)
    }

    internal fun automatiskAvvisning(
        publiserer: Publiserer,
        vedtaksperiodeId: UUID,
        begrunnelser: List<String>,
        utbetaling: Utbetaling,
        hendelseId: UUID,
    ) {
        val godkjenningsbehovJson = meldingDao.finnUtbetalingsgodkjenningbehovJson(hendelseId)
        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
        val fødselsnummer = meldingDao.finnFødselsnummer(hendelseId)
        automatiskAvvisning(publiserer, behov, vedtaksperiodeId, fødselsnummer, begrunnelser, hendelseId)
    }

    private fun automatiskAvvisning(
        publiserer: Publiserer,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        begrunnelser: List<String>,
        hendelseId: UUID,
    ) {
        behov.avvisAutomatisk(begrunnelser)
        publiserer.publiser(behov.toJson())
        publiserer.publiser(behov.lagVedtaksperiodeAvvistAutomatisk(vedtaksperiodeId, fødselsnummer, vedtakDao).toJson())
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer,
            AutomatiskBehandlingPayload(hendelseId, AutomatiskBehandlingUtfall.AVVIST),
            OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV
        )

        begrunnelser.forEach { automatiskAvvistÅrsakerTeller.labels(it).inc() }
        automatiseringsteller.inc()
        sikkerLogg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga: $begrunnelser")
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}

fun interface Publiserer {
    fun publiser(melding: String)
}
