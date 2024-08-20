package no.nav.helse.mediator

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
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingPayload
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingUtfall
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class GodkjenningMediator(
    private val vedtakDao: VedtakDao,
    private val opptegnelseDao: OpptegnelseDao,
    val oppgaveDao: OppgaveDao,
    val utbetalingDao: UtbetalingDao,
    val meldingDao: MeldingDao,
    val generasjonDao: GenerasjonDao,
) {
    internal fun saksbehandlerUtbetaling(
        behandlingId: UUID,
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
        spleisBehandlingId: UUID?,
    ) {
        behov.godkjennManuelt(
            behandlingId = behandlingId,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )
        sykefraværstilfelle.håndterGodkjent(vedtaksperiodeId)

        context.publiser(behov.toJson())
        context.publiser(
            behov.lagVedtaksperiodeGodkjentManuelt(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                vedtakDao = vedtakDao,
            ).toJson(),
        )
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
        spleisBehandlingId: UUID?,
    ) {
        behov.avvisManuelt(
            behandlingId = behandlingId,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )
        context.publiser(behov.toJson())
        context.publiser(
            behov.lagVedtaksperiodeAvvistManuelt(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                vedtakDao = vedtakDao,
            ).toJson(),
        )
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        hendelseId: UUID,
        spleisBehandlingId: UUID?,
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        context.publiser(
            behov.lagVedtaksperiodeGodkjentAutomatisk(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                vedtakDao = vedtakDao,
            ).toJson(),
        )
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = fødselsnummer,
            payload = AutomatiskBehandlingPayload(hendelseId, AutomatiskBehandlingUtfall.UTBETALT),
            type = OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        )
        automatiseringsteller.inc()
        sikkerLogg.info(
            "Automatisk godkjenning av vedtaksperiode $vedtaksperiodeId for {}",
            keyValue("fødselsnummer", fødselsnummer),
        )
    }

    internal fun automatiskAvvisning(
        publiserer: Publiserer,
        begrunnelser: List<String>,
        oppgaveId: Long,
    ) {
        val utbetaling = utbetalingDao.utbetalingFor(oppgaveId) ?: return
        val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
        val hendelseId = oppgaveDao.finnGodkjenningsbehov(fødselsnummer)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer)
        val spleisBehandlingId = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)?.spleisBehandlingId
        automatiskAvvisning(
            publiserer = publiserer,
            vedtaksperiodeId = vedtaksperiodeId,
            begrunnelser = begrunnelser,
            utbetaling = utbetaling,
            hendelseId = hendelseId,
            spleisBehandlingId = spleisBehandlingId,
        )
    }

    internal fun automatiskAvvisning(
        publiserer: Publiserer,
        vedtaksperiodeId: UUID,
        begrunnelser: List<String>,
        utbetaling: Utbetaling,
        hendelseId: UUID,
        spleisBehandlingId: UUID?,
    ) {
        val godkjenningsbehovJson = meldingDao.finnUtbetalingsgodkjenningbehovJson(hendelseId)
        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
        val fødselsnummer = meldingDao.finnFødselsnummer(hendelseId)
        automatiskAvvisning(
            publiserer = publiserer,
            behov = behov,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            begrunnelser = begrunnelser,
            hendelseId = hendelseId,
            spleisBehandlingId = spleisBehandlingId,
        )
    }

    private fun automatiskAvvisning(
        publiserer: Publiserer,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        begrunnelser: List<String>,
        hendelseId: UUID,
        spleisBehandlingId: UUID?,
    ) {
        behov.avvisAutomatisk(begrunnelser)
        publiserer.publiser(behov.toJson())
        publiserer.publiser(
            behov.lagVedtaksperiodeAvvistAutomatisk(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                vedtakDao = vedtakDao,
            ).toJson(),
        )
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = fødselsnummer,
            payload = AutomatiskBehandlingPayload(hendelseId, AutomatiskBehandlingUtfall.AVVIST),
            type = OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
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
