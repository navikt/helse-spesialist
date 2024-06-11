package no.nav.helse.opprydding

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
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
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "slett_person")
                it.requireKey("@id", "fødselsnummer")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        sikkerlogg.info("Sletter person med fødselsnummer: $fødselsnummer")
        sendKommandokjederAvbrutt(fødselsnummer, context)
        personRepository.slett(fødselsnummer)
    }

    private fun sendKommandokjederAvbrutt(
        fødselsnummer: String,
        context: MessageContext,
    ) = finnAktiveKommandokjeder(fødselsnummer).map(::lagMelding).forEach(context::publish)

    private fun lagMelding(kommandokjedeinfo: CommandContextDao.Kommandokjedeinfo) =
        mapOf(
            "@event_name" to "kommandokjede_avbrutt",
            "commandContextId" to kommandokjedeinfo.contextId,
            "meldingId" to kommandokjedeinfo.hendelseId,
        ).let(objectMapper::writeValueAsString)

    private fun finnAktiveKommandokjeder(fødselsnummer: String) = commandContextDao.finnAktiveKommandokjeder(fødselsnummer)
}
