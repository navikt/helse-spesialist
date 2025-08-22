package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterInfotrygdutbetalingerHardt(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val førsteKjenteDagFinner: () -> LocalDate?,
) : Command {
    override fun execute(context: CommandContext) = behandle(context, personDao, fødselsnummer)

    override fun resume(context: CommandContext) = behandle(context, personDao, fødselsnummer)

    private fun behandle(
        context: CommandContext,
        personDao: PersonDao,
        fødselsnummer: String,
    ): Boolean {
        val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
        log.info("Lagrer utbetalinger fra Infotrygd")
        utbetalinger.oppdater(personDao, fødselsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        val førsteKjenteDag = førsteKjenteDagFinner()
        if (førsteKjenteDag == null) {
            log.warn("Hopper over behov for Infotrygdutbetalinger - har ingen kjent dato å starte uthentingen fra")
            return true
        }
        log.info("Etterspør Infotrygdutbetalinger")
        context.behov(
            Behov.Infotrygdutbetalinger(
                fom = førsteKjenteDag.minusYears(3),
                tom = LocalDate.now(),
            ),
        )
        return false
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }
}
