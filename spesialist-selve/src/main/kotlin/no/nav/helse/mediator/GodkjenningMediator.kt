package no.nav.helse.mediator

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.automatiseringsteller
import no.nav.helse.automatiskAvvistÅrsakerTeller
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import org.slf4j.LoggerFactory

internal class GodkjenningMediator(
    private val vedtakDao: VedtakDao,
    private val opptegnelseDao: OpptegnelseDao
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
        godkjenttidspunkt: LocalDateTime,
        saksbehandleroverstyringer: List<UUID>,
        sykefraværstilfelle: Sykefraværstilfelle
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
        context.publiser(behov.lagVedtaksperiodeGodkjent(vedtaksperiodeId, fødselsnummer, vedtakDao).toJson())
    }

    internal fun saksbehandlerAvvisning(
        behandlingId: UUID,
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
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
        context.publiser(behov.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, vedtakDao).toJson())
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        hendelseId: UUID
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeGodkjent(vedtaksperiodeId, fødselsnummer, vedtakDao).toJson())
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, fødselsnummer)
        automatiseringsteller.inc()
        sikkerLogg.info("Automatisk godkjenning av vedtaksperiode $vedtaksperiodeId", keyValue("fødselsnummer", fødselsnummer))
    }

    internal fun automatiskAvvisning(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        begrunnelser: List<String>,
        hendelseId: UUID
    ) {
        behov.avvisAutomatisk(begrunnelser)
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, vedtakDao).toJson())
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, fødselsnummer)
        begrunnelser.forEach { automatiskAvvistÅrsakerTeller.labels(it).inc() }
        automatiseringsteller.inc()
        sikkerLogg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$begrunnelser", keyValue("fødselsnummer", fødselsnummer))
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
