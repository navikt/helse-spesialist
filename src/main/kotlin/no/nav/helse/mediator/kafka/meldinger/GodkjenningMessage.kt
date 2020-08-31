package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.rapids_rivers.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class GodkjenningMessage(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val warnings: List<String>,
    val periodetype: Saksbehandleroppgavetype? = null
) {

    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandAll("@behov", listOf("Godkjenning"))
                    it.rejectKey("@løsning")
                    it.requireKey(
                        "@id", "fødselsnummer", "aktørId", "organisasjonsnummer", "vedtaksperiodeId", "periodeFom",
                        "periodeTom", "warnings"
                    )
                    it.interestedIn("periodetype")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = GodkjenningMessage(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["periodeTom"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
                warnings = packet["warnings"].toWarnings(),
                periodetype = packet["periodetype"].takeUnless { it.isMissingOrNull() }
                    ?.asText()?.let { Saksbehandleroppgavetype.valueOf(it) }
            )
            spleisbehovMediator.håndter(behov, packet.toJson())
        }

        private fun JsonNode.toWarnings() = this["aktiviteter"].map { it["melding"].asText() }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
        }
    }
}
