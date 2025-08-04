package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentPersoninfoløsning
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersoninfoCommand(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val force: Boolean,
) : Command {
    private companion object {
        private val BEHOV = Behov.Personinfo
        private val logg = LoggerFactory.getLogger(OppdaterPersoninfoCommand::class.simpleName)
    }

    override fun execute(context: CommandContext): Boolean {
        if (erOppdatert(personDao, fødselsnummer) && !force) return ignorer()
        return behandle(context, personDao, fødselsnummer)
    }

    override fun resume(context: CommandContext): Boolean = behandle(context, personDao, fødselsnummer)

    private fun ignorer(): Boolean {
        logg.info("har ikke behov for ${BEHOV::class.simpleName}, informasjonen er ny nok")
        return true
    }

    private fun erOppdatert(
        personDao: PersonDao,
        fødselsnummer: String,
    ): Boolean {
        val sistOppdatert = personDao.finnPersoninfoSistOppdatert(fødselsnummer)
        return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(
        context: CommandContext,
        personDao: PersonDao,
        fødselsnummer: String,
    ): Boolean {
        val personinfo = context.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(context)
        logg.info("oppdaterer personinfo")
        personinfo.oppdater(personDao, fødselsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("trenger oppdatert ${BEHOV::class.simpleName}")
        context.behov(BEHOV)
        return false
    }
}
