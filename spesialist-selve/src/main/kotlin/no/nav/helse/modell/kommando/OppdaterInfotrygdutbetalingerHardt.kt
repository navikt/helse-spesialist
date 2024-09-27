package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonRepository
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterInfotrygdutbetalingerHardt(
    private val fødselsnummer: String,
    private val personRepository: PersonRepository,
    private val førsteKjenteDagFinner: () -> LocalDate,
    private val behov: String = "HentInfotrygdutbetalinger",
) : Command {
    override fun execute(context: CommandContext) = behandle(context, personRepository, fødselsnummer)

    override fun resume(context: CommandContext) = behandle(context, personRepository, fødselsnummer)

    private fun behandle(
        context: CommandContext,
        personRepository: PersonRepository,
        fødselsnummer: String,
    ): Boolean {
        val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
        log.info("Lagrer utbetalinger fra Infotrygd")
        utbetalinger.oppdater(personRepository, fødselsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        log.info("Etterspør Infotrygdutbetalinger")
        context.behov(
            behov,
            mapOf(
                "historikkFom" to førsteKjenteDagFinner().minusYears(3),
                "historikkTom" to LocalDate.now(),
            ),
        )
        return false
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }
}
