package no.nav.helse.modell.kommando

import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersoninfoCommand(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val force: Boolean,
) : Command {
    private companion object {
        private const val BEHOV = "HentPersoninfoV2"
        private val log = LoggerFactory.getLogger(BEHOV)
    }

    override fun execute(context: CommandContext): Boolean {
        if (erOppdatert(personDao, fødselsnummer) && !force) return ignorer()
        return behandle(context, personDao, fødselsnummer)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context, personDao, fødselsnummer)
    }

    private fun ignorer(): Boolean {
        log.info("har ikke behov for $BEHOV, informasjonen er ny nok")
        return true
    }

    private fun erOppdatert(
        personDao: PersonDao,
        fødselsnummer: String,
    ): Boolean {
        val sistOppdatert = personDao.findPersoninfoSistOppdatert(fødselsnummer)
        return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(
        context: CommandContext,
        personDao: PersonDao,
        fødselsnummer: String,
    ): Boolean {
        val personinfo = context.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(context)
        log.info("oppdaterer personinfo")
        personinfo.oppdater(personDao, fødselsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        log.info("trenger oppdatert $BEHOV")
        context.behov(BEHOV, emptyMap())
        return false
    }
}
