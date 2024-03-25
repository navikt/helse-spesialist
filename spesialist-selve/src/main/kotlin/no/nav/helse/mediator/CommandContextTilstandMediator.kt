package no.nav.helse.mediator

import no.nav.helse.mediator.meldinger.PersonmeldingOld
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

internal interface CommandContextObserver: UtgåendeMeldingerObserver {
    fun tilstandEndring(hendelse: String) {}
}

internal class CommandContextTilstandMediator: CommandContextObserver {

    private val utgåendeTilstandEndringer = mutableListOf<String>()

    override fun tilstandEndring(hendelse: String) {
        utgåendeTilstandEndringer.add(hendelse)
    }

    internal fun publiserTilstandsendringer(hendelse: PersonmeldingOld, messageContext: MessageContext) {
        publiserTilstandEndringer(hendelse, messageContext)
        utgåendeTilstandEndringer.clear()
    }

    private fun publiserTilstandEndringer(hendelse: PersonmeldingOld, messageContext: MessageContext) {
        utgåendeTilstandEndringer.forEach { utgåendeTilstandEndringer ->
            logg.info("Publiserer CommandContext tilstandendring i forbindelse med ${hendelse.javaClass.simpleName}")
            sikkerlogg.info("Publiserer CommandContext tilstandendring i forbindelse med ${hendelse.javaClass.simpleName}\n{}", utgåendeTilstandEndringer)
            messageContext.publish(hendelse.fødselsnummer(), utgåendeTilstandEndringer)
        }
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
