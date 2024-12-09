package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.kafka.message_builders.behovName
import no.nav.helse.kafka.message_builders.somJsonMessage
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.behov.Behov
import no.nav.helse.modell.hendelse.UtgåendeHendelse
import no.nav.helse.modell.person.PersonObserver
import no.nav.helse.modell.vedtak.Sykepengevedtak
import org.slf4j.LoggerFactory
import java.util.UUID

internal interface UtgåendeMeldingerObserver : PersonObserver {
    fun behov(
        behov: Behov,
        commandContextId: UUID,
    ) {}

    fun hendelse(hendelse: UtgåendeHendelse) {}
}

internal class UtgåendeMeldingerMediator : CommandContextObserver {
    private val behov = mutableMapOf<String, Behov>()
    private val hendelser = mutableListOf<UtgåendeHendelse>()
    private val sykepengevedtak = mutableListOf<Sykepengevedtak>()
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

    override fun sykepengevedtak(sykepengevedtak: Sykepengevedtak) {
        this.sykepengevedtak.add(sykepengevedtak)
    }

    override fun tilstandEndret(event: KommandokjedeEndretEvent) {
        kommandokjedetilstandsendringer.add(event)
    }

    internal fun publiserOppsamledeMeldinger(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        publiserHendelser(hendelse, messageContext)
        publiserBehov(hendelse, messageContext)
        publiserVedtak(messageContext)
        publiserTilstandsendringer(hendelse, messageContext)
        kommandokjedetilstandsendringer.clear()
        sykepengevedtak.clear()
        behov.clear()
        hendelser.clear()
        commandContextId = null
    }

    private fun publiserHendelser(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        hendelser.forEach { utgåendeHendelse ->
            val packet = utgåendeHendelse.somJsonMessage(hendelse.fødselsnummer()).toJson()
            logg.info("Publiserer hendelse i forbindelse med ${hendelse.javaClass.simpleName}")
            sikkerlogg.info("Publiserer hendelse i forbindelse med ${hendelse.javaClass.simpleName}\n{}", packet)
            messageContext.publish(packet)
        }
    }

    private fun publiserVedtak(messageContext: MessageContext) {
        if (sykepengevedtak.isEmpty()) return
        check(sykepengevedtak.size == 1) { "Forventer å publisere kun ett vedtak" }
        val sykepengevedtak = sykepengevedtak.single()
        val json = sykepengevedtak.somJsonMessage()
        logg.info("Publiserer vedtak_fattet for {}", kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId))
        sikkerlogg.info(
            "Publiserer vedtak_fattet for {}, {}, {}",
            kv("fødselsnummer", sykepengevedtak.fødselsnummer),
            kv("organisasjonsnummer", sykepengevedtak.organisasjonsnummer),
            kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId),
        )
        messageContext.publish(json)
        this.sykepengevedtak.clear()
    }

    private fun publiserBehov(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        if (behov.isEmpty()) return
        val contextId = checkNotNull(commandContextId) { "Kan ikke publisere behov uten commandContextId" }
        val packet = behov.values.somJsonMessage(contextId, hendelse.fødselsnummer(), hendelse.id).toJson()
        logg.info("Publiserer behov for ${behov.keys}")
        sikkerlogg.info("Publiserer behov for ${behov.keys}\n{}", packet)
        messageContext.publish(packet)
    }

    private fun publiserTilstandsendringer(
        hendelse: Personmelding,
        messageContext: MessageContext,
    ) {
        kommandokjedetilstandsendringer.forEach { event ->
            val message = JsonMessage.newMessage(event.eventName, event.detaljer()).toJson()
            logg.info(
                "Publiserer CommandContext tilstandendring i forbindelse med ${hendelse.javaClass.simpleName}, ny tilstand: ${event::class.simpleName}",
            )
            sikkerlogg.info(
                "Publiserer CommandContext tilstandendring i forbindelse med ${hendelse.javaClass.simpleName}, ny tilstand: $${event::class.simpleName}\n{}",
                kommandokjedetilstandsendringer,
            )
            messageContext.publish(message)
        }
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
