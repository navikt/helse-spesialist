package no.nav.helse.modell.risiko

import no.nav.helse.mediator.Toggles
import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.alleHarRisikovurdering
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.warningteller
import org.slf4j.LoggerFactory
import java.util.*

internal class RisikoCommand(
    private val vedtaksperiodeId: UUID,
    private val aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
    private val risikovurderingDao: RisikovurderingDao,
    private val warningDao: WarningDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (!Toggles.Risikovurdering.enabled) return true
        if (risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) != null) return true

        aktiveVedtaksperioder.forEach { aktivVedtaksperiode ->
            aktivVedtaksperiode.behov(context, vedtaksperiodeId)
        }

        return false
    }

    override fun resume(context: CommandContext): Boolean {
        if (!Toggles.Risikovurdering.enabled) return true
        val løsning = context.get<Risikovurderingløsning>() ?: return false
        løsning.lagre(risikovurderingDao, vedtaksperiodeId)
        if (løsning.arbeidsuførhetWarning()) {
            val melding =
                "Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes. Se forklaring på vilkårs-siden."
            warningDao.leggTilWarning(vedtaksperiodeId, Warning(melding, WarningKilde.Spesialist))
            warningteller.labels("WARN", melding).inc()
        }
        if (løsning.faresignalWarning()) {
            val melding =
                "Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger."
            warningDao.leggTilWarning(vedtaksperiodeId, Warning(melding, WarningKilde.Spesialist))
            warningteller.labels("WARN", melding).inc()
        }

        return aktiveVedtaksperioder.alleHarRisikovurdering(risikovurderingDao)
    }
}
