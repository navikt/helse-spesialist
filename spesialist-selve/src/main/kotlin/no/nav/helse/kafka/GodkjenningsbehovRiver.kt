package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class GodkjenningsbehovRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireAll("@behov", listOf("Godkjenning"))
            it.requireValue("behandletAvSpinnvill", true)
            it.forbid("@løsning")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "fødselsnummer",
                "organisasjonsnummer",
                "vedtaksperiodeId",
                "utbetalingId",
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
                "Godkjenning.tags",
            )
            it.requireArray("Godkjenning.perioderMedSammeSkjæringstidspunkt") {
                requireKey("vedtaksperiodeId", "behandlingId", "fom", "tom")
            }
            it.interestedIn("avviksvurderingId")
            it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
            it.interestedIn("Godkjenning.orgnummereMedRelevanteArbeidsforhold")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(Godkjenningsbehov(packet), context)
    }
}
