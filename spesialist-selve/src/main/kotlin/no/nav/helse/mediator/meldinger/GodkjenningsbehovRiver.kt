package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
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
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.demandValue("behandletAvSpinnvill", true)
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
                    "Godkjenning.behandlingId",
                )
                it.requireArray("Godkjenning.perioderMedSammeSkjæringstidspunkt") {
                    requireKey("vedtaksperiodeId", "behandlingId", "fom", "tom")
                }
                it.interestedIn("avviksvurderingId")
                it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
                it.interestedIn("Godkjenning.orgnummereMedRelevanteArbeidsforhold", "Godkjenning.tags")
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
            Godkjenningsbehov(packet),
            avviksvurderingId = packet["avviksvurderingId"].takeUnless { it.isMissingOrNull() }
                ?.let { UUID.fromString(it.asText()) },
            vilkårsgrunnlagId = UUID.fromString(packet["Godkjenning.vilkårsgrunnlagId"].asText()),
            context = context
        )
    }
}
