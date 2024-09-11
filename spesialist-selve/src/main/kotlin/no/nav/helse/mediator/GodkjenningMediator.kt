package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.automatiseringsteller
import no.nav.helse.automatiskAvvistÅrsakerTeller
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingPayload
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingUtfall
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class GodkjenningMediator(private val opptegnelseDao: OpptegnelseDao) {
    internal fun saksbehandlerUtbetaling(
        context: CommandContext,
        behov: GodkjenningsbehovData,
        utbetaling: Utbetaling,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        beslutter: Saksbehandlerløsning.Saksbehandler?,
        godkjenttidspunkt: LocalDateTime,
        saksbehandleroverstyringer: List<UUID>,
        sykefraværstilfelle: Sykefraværstilfelle,
    ) {
        behov.godkjennManuelt(
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            utbetaling = utbetaling,
        )
        sykefraværstilfelle.håndterGodkjent(behov.vedtaksperiodeId)

        context.publiser(behov.toJson())
        context.publiser(
            behov.lagVedtaksperiodeGodkjentManuelt(
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            ).toJson(),
        )
    }

    internal fun saksbehandlerAvvisning(
        context: CommandContext,
        behov: GodkjenningsbehovData,
        utbetaling: Utbetaling,
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
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            utbetaling = utbetaling,
        )
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeAvvistManuelt(saksbehandler).toJson())
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: GodkjenningsbehovData,
        utbetaling: Utbetaling,
    ) {
        behov.godkjennAutomatisk(utbetaling)
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeGodkjentAutomatisk().toJson())
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = behov.fødselsnummer,
            payload = AutomatiskBehandlingPayload(behov.id, AutomatiskBehandlingUtfall.UTBETALT),
            type = OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        )
        automatiseringsteller.inc()
        sikkerLogg.info(
            "Automatisk godkjenning av vedtaksperiode ${behov.vedtaksperiodeId} for {}",
            keyValue("fødselsnummer", behov.fødselsnummer),
        )
    }

    internal fun automatiskAvvisning(
        publiserer: Publiserer,
        begrunnelser: List<String>,
        utbetaling: Utbetaling,
        godkjenningsbehov: GodkjenningsbehovData,
    ) {
        godkjenningsbehov.avvisAutomatisk(
            utbetaling = utbetaling,
            begrunnelser = begrunnelser,
        )
        publiserer.publiser(godkjenningsbehov.toJson())
        publiserer.publiser(
            godkjenningsbehov.lagVedtaksperiodeAvvistAutomatisk().toJson(),
        )
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = godkjenningsbehov.fødselsnummer,
            payload = AutomatiskBehandlingPayload(godkjenningsbehov.id, AutomatiskBehandlingUtfall.AVVIST),
            type = OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        )
        begrunnelser.forEach { automatiskAvvistÅrsakerTeller.labels(it).inc() }
        automatiseringsteller.inc()
        sikkerLogg.info("Automatisk avvisning av vedtaksperiode ${godkjenningsbehov.vedtaksperiodeId} pga: $begrunnelser")
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}

fun interface Publiserer {
    fun publiser(melding: String)
}
