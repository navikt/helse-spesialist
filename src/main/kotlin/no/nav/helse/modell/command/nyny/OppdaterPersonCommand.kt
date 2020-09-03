package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersonCommand(
    fødselsnummer: String,
    personDao: PersonDao
) : MacroCommand() {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

    override val commands: List<Command> = listOf(
        OppdaterPersoninfoCommand(fødselsnummer, personDao),
        OppdaterEnhetCommand(fødselsnummer, personDao),
        OppdaterInfotrygdutbetalingerCommand(fødselsnummer, personDao)
    )

    private abstract class OppdaterCommand(private val fødselsnummer: String, private val personDao: PersonDao) : Command {
        override fun execute(context: CommandContext): Boolean {
            if (erOppdatert(personDao, fødselsnummer)) return ignorer()
            return behandle(context, personDao, fødselsnummer)
        }

        override fun resume(context: CommandContext): Boolean {
            return behandle(context, personDao, fødselsnummer)
        }

        private fun ignorer(): Boolean {
            log.info("informasjonen er ny nok")
            return true
        }

        protected abstract fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean

        protected abstract fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean
    }

    private class OppdaterPersoninfoCommand(fødselsnummer: String, personDao: PersonDao) : OppdaterCommand(fødselsnummer, personDao) {
        override fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean {
            val sistOppdatert = personDao.findPersoninfoSistOppdatert(fødselsnummer)
            return sistOppdatert > LocalDate.now().minusDays(14)
        }

        override fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
            val personinfo = context.get<HentPersoninfoLøsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer personinfo")
            personinfo.oppdater(personDao, fødselsnummer)
            return true
        }

        private fun trengerMerInformasjon(context: CommandContext): Boolean {
            log.info("trenger oppdatert personinfo")
            context.behov("HentPersoninfo")
            return false
        }
    }

    private class OppdaterEnhetCommand(fødselsnummer: String, personDao: PersonDao) : OppdaterCommand(fødselsnummer, personDao) {
        override fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean {
            val sistOppdatert = personDao.findEnhetSistOppdatert(fødselsnummer)
            return sistOppdatert > LocalDate.now().minusDays(5)
        }

        override fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
            val enhet = context.get<HentEnhetLøsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer enhetsnr")
            enhet.oppdater(personDao, fødselsnummer)
            return true
        }

        private fun trengerMerInformasjon(context: CommandContext): Boolean {
            log.info("trenger oppdatert enhetsnr")
            context.behov("HentEnhet")
            return false
        }
    }

    private class OppdaterInfotrygdutbetalingerCommand(fødselsnummer: String, personDao: PersonDao) : OppdaterCommand(fødselsnummer, personDao) {
        override fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean {
            val sistOppdatert = personDao.findITUtbetalingsperioderSistOppdatert(fødselsnummer)
            return sistOppdatert > LocalDate.now().minusDays(1)
        }

        override fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
            val utbetalinger = context.get<HentInfotrygdutbetalingerLøsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer utbetalinger fra Infotrygd")
            utbetalinger.oppdater(personDao, fødselsnummer)
            return true
        }

        private fun trengerMerInformasjon(context: CommandContext): Boolean {
            log.info("trenger oppdatert utbetalinger fra Infotrygd")
            context.behov("HentInfotrygdutbetalinger")
            return false
        }
    }
}
