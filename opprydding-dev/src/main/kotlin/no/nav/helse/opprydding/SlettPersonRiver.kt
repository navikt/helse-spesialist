package no.nav.helse.opprydding

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class SlettPersonRiver(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val commandContextDao: CommandContextDao,
) : River.PacketListener {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "slett_person")
                }
                validate {
                    it.requireKey("@id", "fødselsnummer")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        sikkerlogg.info("Sletter person med fødselsnummer: $fødselsnummer")
        sendKommandokjederAvbrutt(fødselsnummer, context)
        personRepository.slett(fødselsnummer)
        sendPersonSlettet(fødselsnummer, context)
    }

    private fun sendKommandokjederAvbrutt(
        fødselsnummer: String,
        context: MessageContext,
    ) = finnAktiveKommandokjeder(fødselsnummer).map(Meldinger::kommandokjedeAvbrutt).forEach(context::publish)

    private fun sendPersonSlettet(
        fødselsnummer: String,
        context: MessageContext,
    ) = Meldinger.personSlettet(fødselsnummer).let { context.publish(it) }

    private fun finnAktiveKommandokjeder(fødselsnummer: String) = commandContextDao.finnAktiveKommandokjeder(fødselsnummer)

    private object Meldinger {
        fun kommandokjedeAvbrutt(kommandokjedeinfo: CommandContextDao.Kommandokjedeinfo): String =
            mapOf(
                "@event_name" to "kommandokjede_avbrutt",
                "commandContextId" to kommandokjedeinfo.contextId,
                "meldingId" to kommandokjedeinfo.hendelseId,
            ).let(objectMapper::writeValueAsString)

        fun personSlettet(fødselsnummer: String): String =
            mapOf(
                "@event_name" to "person_slettet",
                "fødselsnummer" to fødselsnummer,
            ).let(objectMapper::writeValueAsString)
    }
}
