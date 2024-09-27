package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonRepository
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterPersonCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personRepository: PersonRepository,
) : MacroCommand() {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(fødselsnummer, personRepository, force = false),
            OppdaterEnhetCommand(fødselsnummer, personRepository),
            OppdaterInfotrygdutbetalingerCommand(fødselsnummer, personRepository, førsteKjenteDagFinner),
        )

    private abstract class OppdaterCommand(
        private val fødselsnummer: String,
        private val personRepository: PersonRepository,
        private val behov: String,
        private val parametere: Map<String, Any> = emptyMap(),
    ) : Command {
        override fun execute(context: CommandContext): Boolean {
            if (erOppdatert(personRepository, fødselsnummer)) return ignorer()
            return behandle(context, personRepository, fødselsnummer)
        }

        override fun resume(context: CommandContext): Boolean {
            return behandle(context, personRepository, fødselsnummer)
        }

        private fun ignorer(): Boolean {
            log.info("har ikke behov for $behov, informasjonen er ny nok")
            return true
        }

        protected abstract fun erOppdatert(
            personRepository: PersonRepository,
            fødselsnummer: String,
        ): Boolean

        protected abstract fun behandle(
            context: CommandContext,
            personRepository: PersonRepository,
            fødselsnummer: String,
        ): Boolean

        protected fun trengerMerInformasjon(context: CommandContext): Boolean {
            log.info("trenger oppdatert $behov")
            context.behov(behov, parametere)
            return false
        }
    }

    private class OppdaterEnhetCommand(
        fødselsnummer: String,
        personRepository: PersonRepository,
    ) : OppdaterCommand(fødselsnummer, personRepository, "HentEnhet") {
        override fun erOppdatert(
            personRepository: PersonRepository,
            fødselsnummer: String,
        ): Boolean {
            val sistOppdatert = personRepository.finnEnhetSistOppdatert(fødselsnummer)
            return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(5)
        }

        override fun behandle(
            context: CommandContext,
            personRepository: PersonRepository,
            fødselsnummer: String,
        ): Boolean {
            val enhet = context.get<HentEnhetløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer enhetsnr")
            enhet.oppdater(personRepository, fødselsnummer)
            return true
        }
    }

    private class OppdaterInfotrygdutbetalingerCommand(
        fødselsnummer: String,
        personRepository: PersonRepository,
        førsteKjenteDagFinner: () -> LocalDate,
    ) :
        OppdaterCommand(
                fødselsnummer = fødselsnummer,
                personRepository = personRepository,
                behov = "HentInfotrygdutbetalinger",
                parametere =
                    mapOf(
                        "historikkFom" to førsteKjenteDagFinner().minusYears(3),
                        "historikkTom" to LocalDate.now(),
                    ),
            ) {
        override fun erOppdatert(
            personRepository: PersonRepository,
            fødselsnummer: String,
        ): Boolean {
            val sistOppdatert = personRepository.finnITUtbetalingsperioderSistOppdatert(fødselsnummer)
            return sistOppdatert != null && sistOppdatert > LocalDate.now().minusDays(1)
        }

        override fun behandle(
            context: CommandContext,
            personRepository: PersonRepository,
            fødselsnummer: String,
        ): Boolean {
            val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer utbetalinger fra Infotrygd")
            utbetalinger.oppdater(personRepository, fødselsnummer)
            return true
        }
    }
}
