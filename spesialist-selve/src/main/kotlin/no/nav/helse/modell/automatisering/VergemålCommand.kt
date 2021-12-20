package no.nav.helse.modell.vergemal

import no.nav.helse.avvistPåGrunnAvVergemålTeller
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class VergemålCommand(
    val vergemålDao: VergemålDao,
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val godkjenningMediator: GodkjenningMediator,
    val godkjenningsbehovJson: String
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(VergemålCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return if (Toggle.VergemålToggle.enabled) {
            logg.info("Trenger informasjon om vergemål og fullmakter")
            context.behov("Vergemål")
            false
        } else {
            logg.info("Lar være å slå opp på vergemål (togglet av)")
            true
        }
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<Vergemålløsning>() ?: return false

        if (løsning.brukerUnderVergemål()) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            //TODO: Hvor langt ut må denne?
            behov.avvisAutomatisk(listOf("Vergemål"))
            context.publiser(behov.toJson())
            context.publiser(
                godkjenningMediator.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, behov).toJson()
            )
            avvistPåGrunnAvVergemålTeller.inc()
            logg.info("Automatisk avvisning for vedtaksperiode $vedtaksperiodeId")
        }

        løsning.lagre(vergemålDao)
        return true
    }
}
