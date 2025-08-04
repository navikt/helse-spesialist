package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersonCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personDao: PersonDao,
) : MacroCommand() {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(fødselsnummer, personDao, force = false),
            OppdaterEnhetCommand(fødselsnummer, personDao),
            OppdaterInfotrygdutbetalingerCommand(fødselsnummer, personDao, førsteKjenteDagFinner),
        )

    internal abstract class OppdaterCommand protected constructor(
        private val fødselsnummer: String,
        private val personDao: PersonDao,
        private val behov: Behov,
    ) : Command {
        override fun execute(context: CommandContext): Boolean {
            if (erOppdatert(personDao, fødselsnummer)) return ignorer()
            return behandle(context, personDao, fødselsnummer)
        }

        override fun resume(context: CommandContext): Boolean = behandle(context, personDao, fødselsnummer)

        private fun ignorer(): Boolean {
            log.info("har ikke behov for ${behov::class.simpleName}, informasjonen er ny nok")
            return true
        }

        protected abstract fun erOppdatert(
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean

        protected abstract fun behandle(
            context: CommandContext,
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean

        protected fun trengerMerInformasjon(context: CommandContext): Boolean {
            log.info("trenger oppdatert $behov")
            context.behov(behov)
            return false
        }
    }

    internal class OppdaterEnhetCommand(
        fødselsnummer: String,
        personDao: PersonDao,
    ) : OppdaterCommand(fødselsnummer, personDao, Behov.Enhet) {
        override fun erOppdatert(
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean {
            val sistOppdatert = personDao.finnEnhetSistOppdatert(fødselsnummer)
            return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(5)
        }

        override fun behandle(
            context: CommandContext,
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean {
            val enhet = context.get<HentEnhetløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer enhetsnr")
            enhet.oppdater(personDao, fødselsnummer)
            return true
        }
    }

    private class OppdaterInfotrygdutbetalingerCommand(
        fødselsnummer: String,
        personDao: PersonDao,
        førsteKjenteDagFinner: () -> LocalDate,
    ) : OppdaterCommand(
            fødselsnummer = fødselsnummer,
            personDao = personDao,
            behov =
                Behov.Infotrygdutbetalinger(
                    førsteKjenteDagFinner().minusYears(3),
                    LocalDate.now(),
                ),
        ) {
        override fun erOppdatert(
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean {
            val sistOppdatert = personDao.finnITUtbetalingsperioderSistOppdatert(fødselsnummer)
            return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(1)
        }

        override fun behandle(
            context: CommandContext,
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean {
            val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer utbetalinger fra Infotrygd")
            utbetalinger.oppdater(personDao, fødselsnummer)
            return true
        }
    }
}
