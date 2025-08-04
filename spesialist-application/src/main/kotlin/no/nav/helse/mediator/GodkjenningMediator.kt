package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingPayload
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingUtfall
import no.nav.helse.tellAutomatisering
import no.nav.helse.tellAvvistÅrsak
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class GodkjenningMediator(
    private val opptegnelseDao: OpptegnelseDao,
) {
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

        context.hendelse(behov.medLøsning())
        context.hendelse(
            behov.lagVedtaksperiodeGodkjentManuelt(
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            ),
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
        context.hendelse(behov.medLøsning())
        context.hendelse(behov.lagVedtaksperiodeAvvistManuelt(saksbehandler))
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: GodkjenningsbehovData,
        utbetaling: Utbetaling,
    ) {
        behov.godkjennAutomatisk(utbetaling)
        context.hendelse(behov.medLøsning())
        context.hendelse(behov.lagVedtaksperiodeGodkjentAutomatisk())
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = behov.fødselsnummer,
            payload = AutomatiskBehandlingPayload(behov.id, AutomatiskBehandlingUtfall.UTBETALT).toJson(),
            type = OpptegnelseDao.Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        )
        tellAutomatisering()
        sikkerLogg.info(
            "Automatisk godkjenning av vedtaksperiode ${behov.vedtaksperiodeId} for {}",
            keyValue("fødselsnummer", behov.fødselsnummer),
        )
    }

    internal fun automatiskAvvisning(
        context: CommandContext,
        begrunnelser: List<String>,
        utbetaling: Utbetaling,
        behov: GodkjenningsbehovData,
    ) {
        behov.avvisAutomatisk(
            utbetaling = utbetaling,
            begrunnelser = begrunnelser,
        )
        context.hendelse(behov.medLøsning())
        context.hendelse(behov.lagVedtaksperiodeAvvistAutomatisk())
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = behov.fødselsnummer,
            payload = AutomatiskBehandlingPayload(behov.id, AutomatiskBehandlingUtfall.AVVIST).toJson(),
            type = OpptegnelseDao.Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        )
        begrunnelser.forEach { tellAvvistÅrsak(it) }
        tellAutomatisering()
        sikkerLogg.info("Automatisk avvisning av vedtaksperiode ${behov.vedtaksperiodeId} pga: $begrunnelser")
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
