package no.nav.helse.modell.risiko

import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.alleHarRisikovurdering
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import java.util.*

internal class RisikoCommand(
    private val vedtaksperiodeId: UUID,
    private val aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
    private val risikovurderingDao: RisikovurderingDao,
    private val warningDao: WarningDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) != null) return true

        aktiveVedtaksperioder.forEach { aktivVedtaksperiode ->
            aktivVedtaksperiode.behov(context, vedtaksperiodeId)
        }

        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<Risikovurderingløsning>() ?: return false
        løsning.lagre(risikovurderingDao, vedtaksperiodeId)
        if (løsning.arbeidsuførhetWarning()) {
            Warning.leggTilAdvarsel(
                "Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes. Se forklaring på vilkårs-siden.",
                vedtaksperiodeId, WarningKilde.Spesialist, warningDao
            )
        }
        if (løsning.faresignalWarning()) {
            Warning.leggTilAdvarsel(
                "Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger.",
                vedtaksperiodeId, WarningKilde.Spesialist, warningDao
            )
        }

        return aktiveVedtaksperioder.alleHarRisikovurdering(risikovurderingDao)
    }
}
