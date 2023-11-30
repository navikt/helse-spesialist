package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory

internal class OppdaterInfotrygdutbetalingerHardt(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val førsteKjenteDagFinner: () -> LocalDate,
    private val behov: String = "HentInfotrygdutbetalinger",
) : Command {

    override fun execute(context: CommandContext) = behandle(context, personDao, fødselsnummer)
    override fun resume(context: CommandContext) = behandle(context, personDao, fødselsnummer)

    private fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
        val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
        log.info("Lagrer utbetalinger fra Infotrygd")
        utbetalinger.oppdater(personDao, fødselsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        log.info("Etterspør Infotrygdutbetalinger")
        context.behov(
            behov, mapOf(
                "historikkFom" to førsteKjenteDagFinner().minusYears(3),
                "historikkTom" to LocalDate.now()
            )
        )
        return false
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

}
