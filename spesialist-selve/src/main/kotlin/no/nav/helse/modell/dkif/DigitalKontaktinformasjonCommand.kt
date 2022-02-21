package no.nav.helse.modell.dkif

import no.nav.helse.mediator.meldinger.DigitalKontaktinformasjonløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class DigitalKontaktinformasjonCommand(
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val warningDao: WarningDao,
    private val vedtaksperiodeId: UUID
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(DigitalKontaktinformasjonCommand::class.java)
    }

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<DigitalKontaktinformasjonløsning>()
        if (løsning == null) {
            logg.info("Trenger reservasjonsinformasjon fra DKIF")
            context.behov("DigitalKontaktinformasjon")
            return false
        }
        løsning.lagre(digitalKontaktinformasjonDao)
        løsning.evaluer(warningDao, vedtaksperiodeId)
        return true
    }
}
