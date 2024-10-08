package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class GodkjenningsbehovRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandAll("@behov", listOf("Godkjenning"))
            it.demandValue("behandletAvSpinnvill", true)
            it.rejectKey("@løsning")
            it.requireKey(
                "@id",
                "fødselsnummer",
                "aktørId",
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
    ) {
        sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(Godkjenningsbehov(packet), context)
    }
}
