package no.nav.helse.mediator

import no.nav.helse.behovName
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.behov.Behov
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.somJsonMessage
import org.slf4j.LoggerFactory
import java.util.UUID

internal interface UtgåendeMeldingerObserver {
    fun behov(
        behov: Behov,
        commandContextId: UUID,
    ) {}

    fun hendelse(hendelse: String) {}
}

internal class UtgåendeMeldingerMediator : CommandContextObserver {
    private val utgåendeBehov = mutableMapOf<String, Behov>()
    private val utgåendeHendelser = mutableListOf<String>()
    private var commandContextId: UUID? = null

    override fun behov(
        behov: Behov,
        commandContextId: UUID,
    ) {
        this.commandContextId = commandContextId
        utgåendeBehov[behov.behovName()] = behov
    }

    override fun hendelse(hendelse: String) {
        utgåendeHendelser.add(hendelse)
    }

    internal fun publiserOppsamledeMeldinger(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        publiserHendelser(hendelse, messageContext)
        publiserBehov(hendelse, messageContext)
        utgåendeHendelser.clear()
        commandContextId = null
    }

    private fun publiserHendelser(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        utgåendeHendelser.forEach { utgåendeHendelse ->
            logg.info("Publiserer hendelse i forbindelse med ${hendelse.javaClass.simpleName}")
            sikkerlogg.info("Publiserer hendelse i forbindelse med ${hendelse.javaClass.simpleName}\n{}", utgåendeHendelse)
            messageContext.publish(utgåendeHendelse)
        }
    }

    private fun publiserBehov(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        if (utgåendeBehov.isEmpty()) return
        val contextId = checkNotNull(commandContextId) { "Kan ikke publisere behov uten commandContextId" }
        val packet = utgåendeBehov.values.somJsonMessage(contextId, hendelse.fødselsnummer(), hendelse.id).toJson()
        logg.info("Publiserer behov for ${utgåendeBehov.keys}")
        sikkerlogg.info("Publiserer behov for ${utgåendeBehov.keys}\n{}", packet)
        messageContext.publish(packet)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
