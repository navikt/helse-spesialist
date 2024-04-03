package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.VarseldefinisjonMessage
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VarseldefinisjonRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "varselkode_ny_definisjon")
                it.requireKey("@id")
                it.requireKey("varselkode")
                it.requireKey("gjeldende_definisjon")
                it.requireKey(
                    "gjeldende_definisjon.id",
                    "gjeldende_definisjon.kode",
                    "gjeldende_definisjon.tittel",
                    "gjeldende_definisjon.avviklet",
                    "gjeldende_definisjon.opprettet",
                )
                it.interestedIn("gjeldende_definisjon.forklaring", "gjeldende_definisjon.handling")
            }
        }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke varselkode_ny_definisjon:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerlogg.info("Mottok melding om ny definisjon for {}", kv("varselkode", packet["varselkode"].asText()))

        val message = VarseldefinisjonMessage(packet)
        message.sendInnTil(mediator)
    }
}
