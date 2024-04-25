package no.nav.helse.modell.stoppknapp

import no.nav.helse.db.UnntaFraAutomatiseringDao
import no.nav.helse.mediator.erProd
import no.nav.helse.mediator.meldinger.løsninger.AutomatiseringStoppetAvVeilederLøsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime.now

internal class KontrollerStoppknapp(
    private val fødselsnummer: String,
    private val unntaFraAutomatiseringDao: UnntaFraAutomatiseringDao,
) : Command {
    private companion object {
        val logg = LoggerFactory.getLogger(KontrollerStoppknapp::class.java)
    }

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<AutomatiseringStoppetAvVeilederLøsning>()
        return when {
            erProd() -> true
            harOppdatertInformasjon() -> true
            løsning != null -> {
                løsning.lagre(unntaFraAutomatiseringDao)
                true
            }

            else -> {
                logg.info("Trenger informasjon om stoppknapptrykk")
                context.behov("AutomatiseringStoppetAvVeileder")
                false
            }
        }
    }

    private fun harOppdatertInformasjon() =
        unntaFraAutomatiseringDao.sistOppdatert(fødselsnummer)?.let { it > now().minusHours(1) } ?: false
}
