package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk.Companion.toOverstyrteDagerDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class OverstyringTidslinjeRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "saksbehandler_overstyrer_tidslinje")
                it.requireKey("@opprettet")
                it.requireKey("aktørId")
                it.requireKey("fødselsnummer")
                it.requireKey("organisasjonsnummer")
                it.requireKey("dager")
                it.requireKey("@id")
                it.requireKey("saksbehandlerOid")
                it.requireKey("saksbehandlerNavn")
                it.requireKey("saksbehandlerIdent")
                it.requireKey("saksbehandlerEpost")
                it.requireKey("begrunnelse")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        logg.info(
            "Mottok overstyring med {}",
            StructuredArguments.keyValue("eventId", hendelseId)
        )
        sikkerLogg.info(
            "Mottok overstyring med {}, {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
            StructuredArguments.keyValue("hendelse", packet.toJson())
        )
        mediator.overstyringTidslinje(
            id = UUID.fromString(packet["@id"].asText()),
            fødselsnummer = packet["fødselsnummer"].asText(),
            oid = UUID.fromString(packet["saksbehandlerOid"].asText()),
            navn = packet["saksbehandlerNavn"].asText(),
            ident = packet["saksbehandlerIdent"].asText(),
            epost = packet["saksbehandlerEpost"].asText(),
            orgnummer = packet["organisasjonsnummer"].asText(),
            begrunnelse = packet["begrunnelse"].asText(),
            overstyrteDager = packet["dager"].toOverstyrteDagerDto(),
            opprettet = packet["@opprettet"].asLocalDateTime(),
            json = packet.toJson(),
            context = context,
        )
    }
}