package no.nav.helse.modell.dkif

import no.nav.helse.mediator.meldinger.løsninger.DigitalKontaktinformasjonløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory

internal class DigitalKontaktinformasjonCommand(
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
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
        return true
    }
}
