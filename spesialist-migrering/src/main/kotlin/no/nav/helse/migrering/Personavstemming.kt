package no.nav.helse.migrering

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.migrering.db.SparsomDao
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.migrering.domene.Person
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
        private val sparsomDao: SparsomDao,
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
                        requireKey("organisasjonsnummer")
                        requireArray("vedtaksperioder") {
                            requireKey("forkastet", "fom", "tom", "skjæringstidspunkt")
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
            val hendelseId = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val aktørId = packet["aktørId"].asText()
            val person = Person(aktørId, fødselsnummer)
            person.register(spesialistDao)
            person.opprett()
            sikkerlogg.info("Mottatt person_avstemt for {}, {}", keyValue("fødselsnummer", fødselsnummer), keyValue("aktørId", aktørId))
            val arbeidsgivereJson = packet["arbeidsgivere"]
            if (arbeidsgivereJson.isEmpty) {
                sikkerlogg.info(
                    "Person med {} har ingen arbeidsgivere, forsøker ikke å opprette generasjon.",
                    keyValue("fødselsnummer", fødselsnummer)
                )
                return
            }
            arbeidsgivereJson.forEach {
                val organisasjonsnummer = it["organisasjonsnummer"].asText()
                person.håndterNyArbeidsgiver(organisasjonsnummer)
            }
            val vedtaksperioderJson = arbeidsgivereJson.flatMap { it["vedtaksperioder"] }
            if (vedtaksperioderJson.isEmpty()) {
                sikkerlogg.info(
                    "Person med {} har ingen aktive vedtaksperioder, forsøker ikke å opprette generasjon.",
                    keyValue("fødselsnummer", fødselsnummer)
                )
                return
            }

            sikkerlogg.info("Starter migrering av generasjoner og varsler for person med {}", keyValue("fødselsnummer", fødselsnummer))
        }
    }

}