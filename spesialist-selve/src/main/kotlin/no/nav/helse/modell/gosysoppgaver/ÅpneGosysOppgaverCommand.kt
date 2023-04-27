package no.nav.helse.modell.gosysoppgaver

import java.util.UUID
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import org.slf4j.LoggerFactory

internal class ÅpneGosysOppgaverCommand(
    private val hendelseId: UUID,
    private val aktørId: String,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val warningDao: WarningDao,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(ÅpneGosysOppgaverCommand::class.java)
    }

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<ÅpneGosysOppgaverløsning>()
        if (løsning == null) {
            logg.info("Trenger oppgaveinformasjon fra Gosys")
            context.behov("ÅpneOppgaver", mapOf("aktørId" to aktørId))
            return false
        }
        løsning.lagre(åpneGosysOppgaverDao)
        løsning.evaluer(warningDao, vedtaksperiodeId, sykefraværstilfelle, hendelseId)
        return true
    }
}
