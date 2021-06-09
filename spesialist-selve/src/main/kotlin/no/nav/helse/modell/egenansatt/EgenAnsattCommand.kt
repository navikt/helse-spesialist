package no.nav.helse.modell.egenansatt

import no.nav.helse.avvistPåGrunnAvEgenAnsattTeller
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.EgenAnsattløsning
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class EgenAnsattCommand(
    private val egenAnsattDao: EgenAnsattDao,
    private val godkjenningsbehovJson: String,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val godkjenningMediator: GodkjenningMediator,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(EgenAnsattCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Trenger informasjon om egen ansatt")
        context.behov("EgenAnsatt")
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<EgenAnsattløsning>() ?: return false
        val erEgenAnsatt = løsning.evaluer()

        if (erEgenAnsatt) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            behov.avvisAutomatisk(listOf("Egen ansatt"))
            context.publiser(behov.toJson())
            context.publiser(godkjenningMediator.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, behov).toJson())
            avvistPåGrunnAvEgenAnsattTeller.inc()
            logg.info("Automatisk avvisning for vedtaksperiode $vedtaksperiodeId")
        }

        løsning.lagre(egenAnsattDao)
        return true
    }
}
