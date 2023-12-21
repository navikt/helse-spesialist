package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class GodkjenningsbehovRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.rejectKey("@løsning")
                it.requireKey(
                    "@id", "fødselsnummer", "aktørId", "organisasjonsnummer", "vedtaksperiodeId", "utbetalingId"
                )
                it.requireKey(
                    "Godkjenning.periodeFom",
                    "Godkjenning.periodeTom",
                    "Godkjenning.skjæringstidspunkt",
                    "Godkjenning.periodetype",
                    "Godkjenning.førstegangsbehandling",
                    "Godkjenning.inntektskilde",
                    "Godkjenning.kanAvvises",
                    "Godkjenning.vilkårsgrunnlagId",
                )
                // TODO: Require denne når ny avviksvurdering er i prod
                if (Toggle.Avviksvurdering.enabled) it.requireKey("avviksvurderingId") else it.interestedIn("avviksvurderingId")
                it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
                it.interestedIn("Godkjenning.orgnummereMedRelevanteArbeidsforhold")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        logg.info(
            "Mottok godkjenningsbehov med {}",
            StructuredArguments.keyValue("hendelseId", hendelseId)
        )
        sikkerLogg.info(
            "Mottok godkjenningsbehov med {}, {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
            StructuredArguments.keyValue("hendelse", packet.toJson()),
        )
        mediator.godkjenningsbehov(
            message = packet,
            id = hendelseId,
            fødselsnummer = packet["fødselsnummer"].asText(),
            aktørId = packet["aktørId"].asText(),
            organisasjonsnummer = packet["organisasjonsnummer"].asText(),
            periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
            periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
            skjæringstidspunkt = LocalDate.parse(packet["Godkjenning.skjæringstidspunkt"].asText()),
            vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
            utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
            periodetype = Periodetype.valueOf(packet["Godkjenning.periodetype"].asText()),
            førstegangsbehandling = packet["Godkjenning.førstegangsbehandling"].asBoolean(),
            utbetalingtype = Utbetalingtype.valueOf(packet["Godkjenning.utbetalingtype"].asText()),
            inntektskilde = Inntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
            orgnummereMedRelevanteArbeidsforhold = packet["Godkjenning.orgnummereMedRelevanteArbeidsforhold"]
                .takeUnless(JsonNode::isMissingOrNull)
                ?.map { it.asText() } ?: emptyList(),
            kanAvvises = packet["Godkjenning.kanAvvises"].asBoolean(),
            avviksvurderingId = packet["avviksvurderingId"].takeUnless { it.isMissingOrNull() }?.let { UUID.fromString(it.asText()) },
            vilkårsgrunnlagId = UUID.fromString(packet["Godkjenning.vilkårsgrunnlagId"].asText()),
            context = context
        )
    }
}
