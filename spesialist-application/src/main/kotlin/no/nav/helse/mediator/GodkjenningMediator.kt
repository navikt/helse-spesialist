package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingPayload
import no.nav.helse.spesialist.api.abonnement.AutomatiskBehandlingUtfall
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.tellAutomatisering
import no.nav.helse.tellAvvistÅrsak

class GodkjenningMediator(
    private val opptegnelseDao: OpptegnelseDao,
) {
    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: GodkjenningsbehovData,
    ) {
        behov.godkjennAutomatisk()
        context.hendelse(behov.medLøsning())
        context.hendelse(behov.lagVedtaksperiodeGodkjentAutomatisk())
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer = behov.fødselsnummer,
            payload = AutomatiskBehandlingPayload(behov.id, AutomatiskBehandlingUtfall.UTBETALT).toJson(),
            type = OpptegnelseDao.Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        )
        tellAutomatisering()
        sikkerlogg.info(
            "Automatisk godkjenning av vedtaksperiode ${behov.vedtaksperiodeId} for {}",
            keyValue("fødselsnummer", behov.fødselsnummer),
        )
    }

    internal fun automatiskAvvisning(
        context: CommandContext,
        begrunnelser: List<String>,
        behov: GodkjenningsbehovData,
    ) {
        behov.avvisAutomatisk(
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
        sikkerlogg.info("Automatisk avvisning av vedtaksperiode ${behov.vedtaksperiodeId} pga: $begrunnelser")
    }
}
