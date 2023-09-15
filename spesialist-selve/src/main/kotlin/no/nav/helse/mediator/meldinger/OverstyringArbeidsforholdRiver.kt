package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class OverstyringArbeidsforholdRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
): River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "saksbehandler_overstyrer_arbeidsforhold")
                it.requireKey("aktørId")
                it.requireKey("fødselsnummer")
                it.requireKey("skjæringstidspunkt")
                it.requireKey("saksbehandlerOid")
                it.requireKey("@id")
                it.requireKey("@opprettet")
                it.requireArray("overstyrteArbeidsforhold") {
                    requireKey("orgnummer")
                    requireKey("deaktivert")
                    requireKey("begrunnelse")
                    requireKey("forklaring")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        logg.info(
            "Mottok overstyring av arbeidsforhold med {}",
            StructuredArguments.keyValue("eventId", hendelseId)
        )
        sikkerLogg.info(
            "Mottok overstyring av arbeidsforhold med {}, {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
            StructuredArguments.keyValue("hendelse", packet.toJson())
        )

        mediator.overstyringArbeidsforhold(
            id = UUID.fromString(packet["@id"].asText()),
            fødselsnummer = packet["fødselsnummer"].asText(),
            oid = UUID.fromString(packet["saksbehandlerOid"].asText()),
            overstyrteArbeidsforhold = packet["overstyrteArbeidsforhold"].map {
                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdDto(
                    it["orgnummer"].asText(),
                    it["deaktivert"].asBoolean(),
                    it["begrunnelse"].asText(),
                    it["forklaring"].asText()
                )
            },
            skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
            opprettet = packet["@opprettet"].asLocalDateTime(),
            json = packet.toJson(),
            context = context
        )
    }
}