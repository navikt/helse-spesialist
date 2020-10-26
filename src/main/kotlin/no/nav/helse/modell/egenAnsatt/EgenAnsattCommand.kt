package no.nav.helse.modell.egenAnsatt

import no.nav.helse.avvistPåGrunnAvEgenAnsattTeller
import no.nav.helse.mediator.MiljøstyrtFeatureToggle
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
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(EgenAnsattCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.egenAnsatt()) return true

        logg.info("Trenger informasjon om egen ansatt")
        context.behov("EgenAnsatt")
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.egenAnsatt()) return true

        val løsning = context.get<EgenAnsattløsning>() ?: return false
        val erEgenAnsatt = løsning.evaluer()

        if (erEgenAnsatt) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            behov.avvisAutomatisk(listOf("Egen ansatt"))
            context.publiser(behov.toJson())
            avvistPåGrunnAvEgenAnsattTeller.inc()
            logg.info("Automatisk avvisning for vedtaksperiode $vedtaksperiodeId")
        }

        løsning.lagre(egenAnsattDao)
        return true
    }
}
