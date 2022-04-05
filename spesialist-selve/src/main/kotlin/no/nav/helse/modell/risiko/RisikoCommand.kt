package no.nav.helse.modell.risiko

import java.time.LocalDateTime
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.tellWarning
import org.slf4j.LoggerFactory
import java.util.UUID

internal class RisikoCommand(
    private val vedtaksperiodeId: UUID,
    private val risikovurderingDao: RisikovurderingDao,
    private val warningDao: WarningDao,
    private val organisasjonsnummer: String,
    private val periodetype: Periodetype
) : Command {

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        if (risikovurderingAlleredeGjort()) return true

        val løsning = context.get<Risikovurderingløsning>()
        if (løsning == null || !løsning.gjelderVedtaksperiode(vedtaksperiodeId)) {
            logg.info("Trenger risikovurdering av vedtaksperiode $vedtaksperiodeId")
            context.behov("Risikovurdering", mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "organisasjonsnummer" to organisasjonsnummer,
                "periodetype" to periodetype
            ))
            return false
        }

        løsning.lagre(risikovurderingDao)
        løsning.leggTilWarnings()
        return true
    }

    private fun risikovurderingAlleredeGjort() = risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) != null

    private fun Risikovurderingløsning.leggTilWarnings() {
        if (harArbeidsuførhetFunn()) {
            val melding = arbeidsuførhetsmelding()
            warningDao.leggTilWarning(
                vedtaksperiodeId, Warning(
                    melding = melding,
                    kilde = WarningKilde.Spesialist,
                    opprettet = LocalDateTime.now(),
                )
            )
            tellWarning(melding)
        }
        if (harFaresignalerFunn()) {
            val melding = "Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger."
            warningDao.leggTilWarning(
                vedtaksperiodeId, Warning(
                    melding = melding,
                    kilde = WarningKilde.Spesialist,
                    opprettet = LocalDateTime.now(),
                )
            )
            tellWarning(melding)
        }
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(RisikoCommand::class.java)
    }
}
