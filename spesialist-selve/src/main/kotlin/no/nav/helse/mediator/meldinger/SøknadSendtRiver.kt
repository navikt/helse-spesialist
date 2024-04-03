package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class SøknadSendtRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny("@event_name", listOf("sendt_søknad_arbeidsgiver", "sendt_søknad_nav"))
                it.requireKey(
                    "@id",
                    "fnr",
                    "aktorId",
                    "arbeidsgiver.orgnummer",
                )
            }
        }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke SøknadSendt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logg.info(
            "Mottok SøknadSendt med {}",
            keyValue("hendelseId", UUID.fromString(packet["@id"].asText())),
        )
        sikkerLogg.info(
            "Mottok SøknadSendt med {}, {}",
            keyValue("hendelseId", UUID.fromString(packet["@id"].asText())),
            keyValue("hendelse", packet.toJson()),
        )
        mediator.håndter(SøknadSendt.søknadSendt(packet), context)
    }
}
