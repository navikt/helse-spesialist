package no.nav.helse.modell.kommando

import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.spesialist.application.InfotrygdutbetalingerRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterInfotrygdutbetalingerHardt(
    private val fødselsnummer: String,
    private val infotrygdutbetalingerRepository: InfotrygdutbetalingerRepository,
    private val førsteKjenteDagFinner: () -> LocalDate?,
) : Command {
    override fun execute(context: CommandContext) = behandle(context, fødselsnummer)

    override fun resume(context: CommandContext) = behandle(context, fødselsnummer)

    private fun behandle(
        context: CommandContext,
        fødselsnummer: String,
    ): Boolean {
        val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
        log.info("Lagrer utbetalinger fra Infotrygd")
        utbetalinger.oppdater(infotrygdutbetalingerRepository, fødselsnummer)
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
