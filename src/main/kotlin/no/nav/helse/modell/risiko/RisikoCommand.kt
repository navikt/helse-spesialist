package no.nav.helse.modell.risiko

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtak.WarningKilde
import org.slf4j.LoggerFactory
import java.util.*

internal class RisikoCommand(
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val risikovurderingDao: RisikovurderingDao,
    private val warningDao: WarningDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(RisikoCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.risikovurdering()) return true
        logg.info("Trenger risikovurdering for {}", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        context.behov("Risikovurdering", mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "organisasjonsnummer" to organisasjonsnummer
        ))
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.risikovurdering()) return true
        val løsning = context.get<Risikovurderingløsning>() ?: return false
        logg.info("Mottok risikovurdering for {}", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        løsning.lagre(risikovurderingDao)
        if (løsning.medførerWarning()) {
            warningDao.leggTilWarning(vedtaksperiodeId, WarningDto("Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes", WarningKilde.Spesialist))
        }
        return true
    }
}
