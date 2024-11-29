package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.mediator.meldinger.Personmelding
import org.slf4j.LoggerFactory

internal interface CommandContextObserver : UtgåendeMeldingerObserver {
    fun tilstandEndret(event: KommandokjedeEndretEvent) {}
}

internal class CommandContextTilstandMediator : CommandContextObserver {
    private val utgåendeTilstandEndringer = mutableListOf<Pair<String, String>>()

    override fun tilstandEndret(event: KommandokjedeEndretEvent) {
        val message = JsonMessage.newMessage(event.eventName, event.detaljer()).toJson()
        utgåendeTilstandEndringer.add(event::class.simpleName!! to message)
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
            messageContext.publish(utgåendeTilstandEndringer)
        }
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
