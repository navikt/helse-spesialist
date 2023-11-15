package no.nav.helse.modell.egenansatt

import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory

internal class EgenAnsattCommand(
    private val fødselsnummer: String,
    private val egenAnsattDao: EgenAnsattDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(EgenAnsattCommand::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<EgenAnsattløsning>()
        if (løsning == null) {
            logg.info("Trenger informasjon om egen ansatt")
            context.behov("EgenAnsatt")
            return false
        }

        val statusFørUpdate = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        løsning.lagre(egenAnsattDao)
        if (statusFørUpdate != null) {
            val statusEtterUpdate = egenAnsattDao.erEgenAnsatt(fødselsnummer)
            val nødvendigEllerIkke = if (statusFørUpdate == statusEtterUpdate) "unødvendig" else "nødvendig"
            sikkerlogg.debug("Behov 'EgenAnsatt' var $nødvendigEllerIkke. Før update var status '$statusFørUpdate', etter update er status '$statusEtterUpdate'")
        }
        return true
    }
}
