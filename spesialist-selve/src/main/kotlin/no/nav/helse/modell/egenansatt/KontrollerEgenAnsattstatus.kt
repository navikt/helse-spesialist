package no.nav.helse.modell.egenansatt

import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory

internal class KontrollerEgenAnsattstatus(
    private val fødselsnummer: String,
    private val egenAnsattDao: EgenAnsattDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(KontrollerEgenAnsattstatus::class.java)
    }

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        if (viHarInformasjon()) return true
        val løsning = context.get<EgenAnsattløsning>()
        if (løsning == null) {
            logg.info("Trenger informasjon om egen ansatt")
            context.behov("EgenAnsatt")
            return false
        }

        løsning.lagre(egenAnsattDao)
        return true
    }

    // Hvis vi har informasjon i databasen er den "garantert" oppdatert, pga. at vi lytter på endringer på topic fra NOM.
    private fun viHarInformasjon() = egenAnsattDao.erEgenAnsatt(fødselsnummer) != null
}
