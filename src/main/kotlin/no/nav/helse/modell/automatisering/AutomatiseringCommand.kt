package no.nav.helse.modell.automatisering

import no.nav.helse.automatiseringsteller
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class AutomatiseringCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val vurdering = automatisering.vurder(fødselsnummer, vedtaksperiodeId)

        if (vurdering.erAutomatiserbar()) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            behov.godkjennAutomatisk()
            context.publiser(behov.toJson())
            automatiseringsteller.inc()
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
        }

        automatisering.lagre(vurdering, vedtaksperiodeId, hendelseId)
        return true
    }
}
