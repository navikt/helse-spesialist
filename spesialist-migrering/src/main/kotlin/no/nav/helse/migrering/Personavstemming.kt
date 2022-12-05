package no.nav.helse.migrering

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.migrering.db.SparsomDao
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.migrering.domene.Generasjon.Companion.lagre
import no.nav.helse.migrering.domene.Utbetaling
import no.nav.helse.migrering.domene.Vedtaksperiode
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
                    it.requireKey("@id", "fødselsnummer")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.requireArray("arbeidsgivere") {
                        requireArray("vedtaksperioder") {
                            requireKey("tilstand")
                            require("opprettet", JsonNode::asLocalDateTime)
                            require("oppdatert", JsonNode::asLocalDateTime)
                            require("id") { jsonNode ->
                                UUID.fromString(jsonNode.asText())
                            }
                            requireArray("utbetalinger")
                        }
                        requireArray("utbetalinger") {
                            require("opprettet", JsonNode::asLocalDateTime)
                            require("oppdatert", JsonNode::asLocalDateTime)
                            requireKey("type", "status")
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
            val arbeidsgivereJson = packet["arbeidsgivere"]
            if (arbeidsgivereJson.isEmpty) {
                sikkerlogg.info(
                    "Person med {} har ingen arbeidsgivere, forsøker ikke å opprette generasjon.",
                    keyValue("fødselsnummer", fødselsnummer)
                )
                return
            }
            val vedtaksperioderJson = arbeidsgivereJson.flatMap { it["vedtaksperioder"] }
            if (vedtaksperioderJson.isEmpty()) {
                sikkerlogg.info(
                    "Person med {} har ingen aktive vedtaksperioder, forsøker ikke å opprette generasjon.",
                    keyValue("fødselsnummer", fødselsnummer)
                )
                return
            }
            val varslerForPerson = sparsomDao.finnVarslerFor(fødselsnummer)
            val utbetalingerJson = arbeidsgivereJson.flatMap { it["utbetalinger"] }
            val vedtaksperioder = vedtaksperioderJson.map { periodeNode ->
                val vedtaksperiodeUtbetalinger = utbetalingerJson.filter { utbetalingNode ->
                    utbetalingNode["id"].asText() in periodeNode["utbetalinger"].map { it.asText() }
                }
                Vedtaksperiode(
                    id = UUID.fromString(periodeNode["id"].asText()),
                    opprettet = periodeNode["opprettet"].asLocalDateTime(),
                    oppdatert = periodeNode["oppdatert"].asLocalDateTime(),
                    tilstand = periodeNode["tilstand"].asText(),
                    personVarsler = varslerForPerson,
                    utbetalinger = vedtaksperiodeUtbetalinger.map { utbetalingNode ->
                        Utbetaling(
                            UUID.fromString(utbetalingNode["id"].asText()),
                            utbetalingNode["opprettet"].asLocalDateTime(),
                            utbetalingNode["oppdatert"].asLocalDateTime(),
                            utbetalingNode["status"].asText(),
                        )
                    }
                )
            }
            vedtaksperioder
                .map { periode -> periode.generasjoner().sortedBy { it.opprettet } }
                .forEach { it.lagre(spesialistDao, hendelseId) }
        }
    }

}