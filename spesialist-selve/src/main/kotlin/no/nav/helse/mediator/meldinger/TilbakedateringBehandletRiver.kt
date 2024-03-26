package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// I skrivende stund er det kun meldinger der tilbakedateringen er godkjent som
// kommer til Spesialist, dvs. sendes på rapiden. Andre meldinger filtreres ut i sparkel-appen
internal class TilbakedateringBehandletRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "tilbakedatering_behandlet")
                it.requireKey("@opprettet")
                it.requireKey("@id")
                it.requireKey("fødselsnummer")
                it.requireKey("sykmeldingId")
                it.requireArray("perioder") {
                    requireKey("fom", "tom")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        val sykmeldingId = packet["sykmeldingId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()

        logg.info(
            "Mottok tilbakedatering_behandlet med {}",
            StructuredArguments.keyValue("eventId", hendelseId)
        )
        sikkerlogg.info(
            "Mottok tilbakedatering_behandlet med {}, {}, {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
            StructuredArguments.keyValue("sykmeldingId", sykmeldingId),
            StructuredArguments.keyValue("hendelse", packet.toJson())
        )

        mediator.tilbakedateringBehandlet(fødselsnummer, TilbakedateringBehandlet(packet), context)
    }
}