package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class Varseldefinisjon(
    val id: UUID,
    val kode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val avviklet: Boolean,
    val opprettet: LocalDateTime,
) {

    internal fun lagre(varselRepository: VarselRepository) {
        varselRepository.lagreDefinisjon(id, kode, tittel, forklaring, handling, avviklet, opprettet)
    }

    internal companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Varseldefinisjon>.lagre(varselRepository: VarselRepository) {
            forEach { it.lagre(varselRepository) }
        }

        internal fun JsonNode.definisjoner(): List<Varseldefinisjon> {
            return this
                .map { jsonNode ->
                    Varseldefinisjon(
                        UUID.fromString(jsonNode["id"].asText()),
                        jsonNode["kode"].asText(),
                        jsonNode["tittel"].asText(),
                        jsonNode["forklaring"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                        jsonNode["handling"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                        jsonNode["avviklet"].asBoolean(),
                        jsonNode["opprettet"].asLocalDateTime()
                    )
                }
        }
    }

    internal class River(
        rapidsConnection: RapidsConnection,
        private val varselRepository: VarselRepository
    ) : PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "varseldefinisjoner_endret")
                    it.requireKey("@id")
                    it.requireArray("definisjoner") {
                        this.requireKey("id", "kode", "tittel", "forklaring", "handling", "avviklet")
                        this.require("opprettet", JsonNode::asLocalDateTime)
                    }
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke varseldefinisjoner_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding om varseldefinisjoner endret")

            val definisjoner = packet["definisjoner"].definisjoner()

            definisjoner.lagre(varselRepository)
        }
    }
}
