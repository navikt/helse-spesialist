package no.nav.helse.modell.gosysoppgaver

import no.nav.helse.mediator.meldinger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class ÅpneGosysOppgaverCommand(
    private val aktørId: String,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val warningDao: WarningDao,
    private val vedtaksperiodeId: UUID
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(ÅpneGosysOppgaverCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Trenger oppgaveinformasjon fra Gosys")
        context.behov(
            "ÅpneOppgaver", mapOf(
                "aktørId" to aktørId
            )
        )
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<ÅpneGosysOppgaverløsning>() ?: return false
        løsning.lagre(åpneGosysOppgaverDao)
        løsning.evaluer(warningDao, vedtaksperiodeId)
        return true
    }
}
