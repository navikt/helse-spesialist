package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class SøknadSendtRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation { message ->
            message.demandAny("@event_name", listOf("sendt_søknad_arbeidsgiver", "sendt_søknad_nav"))
            message.requireKey(
                "@id",
                "fnr",
                "aktorId",
                "arbeidsgiver.orgnummer",
            )
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
