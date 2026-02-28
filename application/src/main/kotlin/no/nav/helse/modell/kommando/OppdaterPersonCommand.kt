package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersonCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate?,
    personDao: PersonDao,
    personRepository: PersonRepository,
) : MacroCommand() {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(Identitetsnummer.fraString(fødselsnummer), personRepository, force = false),
            OppdaterEnhetCommand(fødselsnummer, personRepository),
            OppdaterInfotrygdutbetalingerCommand(fødselsnummer, personDao, førsteKjenteDagFinner),
        )

    private class OppdaterInfotrygdutbetalingerCommand(
        private val fødselsnummer: String,
        private val personDao: PersonDao,
        private val førsteKjenteDagFinner: () -> LocalDate?,
    ) : Command {
        override fun execute(context: CommandContext): Boolean {
            if (erOppdatert(personDao, fødselsnummer)) return ignorer()
            return behandle(context, personDao, fødselsnummer)
        }

        override fun resume(context: CommandContext): Boolean = behandle(context, personDao, fødselsnummer)

        private fun ignorer(): Boolean {
            log.info("har ikke behov for Infotrygdutbetalinger, informasjonen er ny nok")
            return true
        }

        private fun trengerMerInformasjon(context: CommandContext): Boolean {
            val førsteKjenteDag = førsteKjenteDagFinner()
            if (førsteKjenteDag == null) {
                log.warn("Hopper over behov for Infotrygdutbetalinger - har ingen kjent dato å starte uthentingen fra")
                return true
            }
            val behov =
                Behov.Infotrygdutbetalinger(
                    førsteKjenteDag.minusYears(3),
                    LocalDate.now(),
                )
            log.info("trenger oppdatert $behov")
            context.behov(behov)
            return false
        }

        private fun erOppdatert(
            personDao: PersonDao,
            fødselsnummer: String,
        ): Boolean {
            val sistOppdatert = personDao.finnITUtbetalingsperioderSistOppdatert(fødselsnummer)
            return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(1)
        }

        private fun behandle(
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
