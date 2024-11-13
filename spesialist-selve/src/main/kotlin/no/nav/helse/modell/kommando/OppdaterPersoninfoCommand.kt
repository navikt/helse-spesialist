package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonRepository
import no.nav.helse.modell.behov.Behov
import no.nav.helse.modell.person.HentPersoninfoløsning
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersoninfoCommand(
    private val fødselsnummer: String,
    private val personRepository: PersonRepository,
    private val force: Boolean,
) : Command {
    private companion object {
        private val BEHOV = Behov.Personinfo
        private val logg = LoggerFactory.getLogger(OppdaterPersoninfoCommand::class.simpleName)
    }

    override fun execute(context: CommandContext): Boolean {
        if (erOppdatert(personRepository, fødselsnummer) && !force) return ignorer()
        return behandle(context, personRepository, fødselsnummer)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context, personRepository, fødselsnummer)
    }

    private fun ignorer(): Boolean {
        logg.info("har ikke behov for ${BEHOV::class.simpleName}, informasjonen er ny nok")
        return true
    }

    private fun erOppdatert(
        personRepository: PersonRepository,
        fødselsnummer: String,
    ): Boolean {
        val sistOppdatert = personRepository.finnPersoninfoSistOppdatert(fødselsnummer)
        return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(
        context: CommandContext,
        personRepository: PersonRepository,
        fødselsnummer: String,
    ): Boolean {
        val personinfo = context.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(context)
        logg.info("oppdaterer personinfo")
        personinfo.oppdater(personRepository, fødselsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("trenger oppdatert ${BEHOV::class.simpleName}")
        context.behov(BEHOV)
        return false
    }
}
