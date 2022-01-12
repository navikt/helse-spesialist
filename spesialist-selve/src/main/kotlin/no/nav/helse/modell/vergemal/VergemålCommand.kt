package no.nav.helse.modell.vergemal

import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory

internal class VergemålCommand(
    val vergemålDao: VergemålDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(VergemålCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return if (Toggle.VergemålToggle.enabled) {
            logg.info("Trenger informasjon om vergemål og fullmakter")
            context.behov("Vergemål")
            false
        } else {
            logg.info("Lar være å slå opp på vergemål (togglet av)")
            true
        }
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<Vergemålløsning>() ?: return false
        løsning.lagre(vergemålDao)
        return true
    }
}
