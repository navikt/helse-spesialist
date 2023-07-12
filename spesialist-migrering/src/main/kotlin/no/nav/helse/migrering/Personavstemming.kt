package no.nav.helse.migrering

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class Personavstemming {

    internal class River(
        rapidsConnection: RapidsConnection,
        private val spesialistDao: SpesialistDao,
    ) : PacketListener {

        private companion object {
            private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        }

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "person_avstemt")
                    it.requireKey("@id", "fødselsnummer", "aktørId")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.requireArray("arbeidsgivere") {
                        requireArray("forkastedeVedtaksperioder") {
                            requireKey("fom", "tom", "skjæringstidspunkt")
                            require("opprettet", JsonNode::asLocalDateTime)
                            require("oppdatert", JsonNode::asLocalDateTime)
                            require("id") { jsonNode ->
                                UUID.fromString(jsonNode.asText())
                            }
                        }
                    }
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val fødselsnummer = packet["fødselsnummer"].asText()
            val aktørId = packet["aktørId"].asText()
            val forkastedeVedtaksperioderIder = packet["arbeidsgivere"].flatMap { arbeidsgiverNode ->
                arbeidsgiverNode.path("forkastedeVedtaksperioder").map { periodeNode ->
                    UUID.fromString(periodeNode.path("id").asText())
                }
            }
            if (forkastedeVedtaksperioderIder.isEmpty()) {
                sikkerlogg.info("Ingen forkastede perioder for {}, {}", keyValue("fødselsnummer", fødselsnummer), keyValue("aktørId", aktørId))
                return
            }
            forkastedeVedtaksperioderIder.forEach(spesialistDao::forkast)
            sikkerlogg.info("Mottatt person_avstemt for {}, {}", keyValue("fødselsnummer", fødselsnummer), keyValue("aktørId", aktørId))
        }
    }

}