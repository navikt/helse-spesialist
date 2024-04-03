package no.nav.helse.mediator

import no.nav.helse.mediator.meldinger.PersonmeldingOld
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

internal interface UtgåendeMeldingerObserver {
    fun behov(
        behov: String,
        ekstraKontekst: Map<String, Any>,
        detaljer: Map<String, Any>,
    ) {}

    fun hendelse(hendelse: String) {}
}

internal class UtgåendeMeldingerMediator : CommandContextObserver {
    private val utgåendeBehov = mutableMapOf<String, Map<String, Any>>()
    private val utgåendeHendelser = mutableListOf<String>()
    private val ekstraKontekst = mutableMapOf<String, Any>()

    override fun behov(
        behov: String,
        ekstraKontekst: Map<String, Any>,
        detaljer: Map<String, Any>,
    ) {
        this.ekstraKontekst.putAll(ekstraKontekst)
        this.utgåendeBehov[behov] = detaljer
    }

    override fun hendelse(hendelse: String) {
        utgåendeHendelser.add(hendelse)
    }

    internal fun publiserOppsamledeMeldinger(
        hendelse: PersonmeldingOld,
        messageContext: MessageContext,
    ) {
        publiserHendelser(hendelse, messageContext)
        publiserBehov(hendelse, messageContext)
        utgåendeBehov.clear()
        utgåendeHendelser.clear()
        ekstraKontekst.clear()
    }

    private fun publiserHendelser(
        hendelse: PersonmeldingOld,
        messageContext: MessageContext,
    ) {
        utgåendeHendelser.forEach { utgåendeHendelse ->
            logg.info("Publiserer hendelse i forbindelse med ${hendelse.javaClass.simpleName}")
            sikkerlogg.info("Publiserer hendelse i forbindelse med ${hendelse.javaClass.simpleName}\n{}", utgåendeHendelse)
            messageContext.publish(hendelse.fødselsnummer(), utgåendeHendelse)
        }
    }

    private fun publiserBehov(
        hendelse: PersonmeldingOld,
        messageContext: MessageContext,
    ) {
        if (utgåendeBehov.isEmpty()) return
        val packet = behovPacket(hendelse)
        logg.info("Publiserer behov for ${utgåendeBehov.keys}")
        sikkerlogg.info("Publiserer behov for ${utgåendeBehov.keys}\n{}", packet)
        messageContext.publish(hendelse.fødselsnummer(), packet)
    }

    private fun behovPacket(hendelse: PersonmeldingOld) =
        JsonMessage.newNeed(
            utgåendeBehov.keys.toList(),
            mutableMapOf<String, Any>(
                "hendelseId" to hendelse.id,
                "fødselsnummer" to hendelse.fødselsnummer(),
            ).apply {
                putAll(ekstraKontekst)
                putAll(utgåendeBehov)
            },
        ).toJson()

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
