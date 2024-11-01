package no.nav.helse.mediator

import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

internal interface CommandContextObserver : UtgåendeMeldingerObserver {
    fun tilstandEndret(hendelse: KommandokjedeEndretEvent) {}
}

internal class CommandContextTilstandMediator : CommandContextObserver {
    private val utgåendeTilstandEndringer = mutableListOf<Pair<String, String>>()

    override fun tilstandEndret(hendelse: KommandokjedeEndretEvent) {
        val message = JsonMessage.newMessage(hendelse.eventName, hendelse.detaljer()).toJson()
        utgåendeTilstandEndringer.add(hendelse::class.simpleName!! to message)
    }

    internal fun publiserTilstandsendringer(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        publiserTilstandEndringer(hendelse, messageContext)
        utgåendeTilstandEndringer.clear()
    }

    private fun publiserTilstandEndringer(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        utgåendeTilstandEndringer.forEach { (nyTilstand, utgåendeTilstandEndringer) ->
            logg.info(
                "Publiserer CommandContext tilstandendring i forbindelse med ${hendelse.javaClass.simpleName}, ny tilstand: $nyTilstand",
            )
            sikkerlogg.info(
                "Publiserer CommandContext tilstandendring i forbindelse med ${hendelse.javaClass.simpleName}, ny tilstand: $nyTilstand\n{}",
                utgåendeTilstandEndringer,
            )
            messageContext.publish(hendelse.fødselsnummer(), utgåendeTilstandEndringer)
        }
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
