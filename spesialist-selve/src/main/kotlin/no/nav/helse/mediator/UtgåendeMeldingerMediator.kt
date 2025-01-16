package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.MeldingPubliserer
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.message_builders.behovName
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import java.util.UUID

interface UtgåendeMeldingerObserver {
    fun behov(
        behov: Behov,
        commandContextId: UUID,
    ) {
    }

    fun hendelse(hendelse: UtgåendeHendelse) {}
}

class UtgåendeMeldingerMediator : CommandContextObserver {
    private val behov = mutableMapOf<String, Behov>()
    private val hendelser = mutableListOf<UtgåendeHendelse>()
    private val kommandokjedetilstandsendringer = mutableListOf<KommandokjedeEndretEvent>()
    private var commandContextId: UUID? = null

    override fun behov(
        behov: Behov,
        commandContextId: UUID,
    ) {
        this.commandContextId = commandContextId
        this.behov[behov.behovName()] = behov
    }

    override fun hendelse(hendelse: UtgåendeHendelse) {
        hendelser.add(hendelse)
    }

    override fun tilstandEndret(event: KommandokjedeEndretEvent) {
        kommandokjedetilstandsendringer.add(event)
    }

    fun publiserOppsamledeMeldinger(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        publiserOppsamledeMeldinger(hendelse, MessageContextMeldingPubliserer(messageContext))
    }

    fun publiserOppsamledeMeldinger(
        hendelse: Personmelding,
        publiserer: MeldingPubliserer,
    ) {
        publiserHendelser(hendelse, publiserer)
        publiserBehov(hendelse, publiserer)
        publiserTilstandsendringer(hendelse, publiserer)
        kommandokjedetilstandsendringer.clear()
        behov.clear()
        hendelser.clear()
        commandContextId = null
    }

    private fun publiserHendelser(
        hendelse: Personmelding,
        publiserer: MeldingPubliserer,
    ) {
        hendelser.forEach { utgåendeHendelse ->
            publiserer.publiser(
                fødselsnummer = hendelse.fødselsnummer(),
                hendelse = utgåendeHendelse,
                hendelseNavn = hendelse.javaClass.simpleName,
            )
        }
    }

    private fun publiserBehov(
        hendelse: Personmelding,
        publiserer: MeldingPubliserer,
    ) {
        if (behov.isEmpty()) return
        val contextId = checkNotNull(commandContextId) { "Kan ikke publisere behov uten commandContextId" }
        publiserer.publiser(
            hendelseId = hendelse.id,
            commandContextId = contextId,
            fødselsnummer = hendelse.fødselsnummer(),
            behov = behov,
        )
    }

    private fun publiserTilstandsendringer(
        hendelse: Personmelding,
        publiserer: MeldingPubliserer,
    ) {
        kommandokjedetilstandsendringer.forEach { event ->
            publiserer.publiser(
                event = event,
                hendelseNavn = hendelse.javaClass.simpleName,
            )
        }
    }
}
