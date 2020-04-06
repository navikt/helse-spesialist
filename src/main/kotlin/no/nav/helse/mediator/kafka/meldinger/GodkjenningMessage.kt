package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate
import java.util.*

internal class GodkjenningMessage(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate
) {

    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate { it ->
                    it.requireAll("@behov", listOf("Godkjenning"))
                    it.requireKey("fødselsnummer")
                    it.requireKey("aktørId")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                }
            }
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = GodkjenningMessage(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["periodeTom"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            )
            spleisbehovMediator.håndter(behov)
        }
    }
}
