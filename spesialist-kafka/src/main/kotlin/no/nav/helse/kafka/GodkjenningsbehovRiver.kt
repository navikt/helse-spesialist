package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.FeatureToggles
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import java.time.LocalDate
import java.util.UUID

class GodkjenningsbehovRiver(
    private val mediator: MeldingMediator,
    private val featureToggles: FeatureToggles,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireAll("@behov", listOf("Godkjenning"))
            if (!featureToggles.skalBenytteNyAvviksvurderingløype()) {
                it.requireValue("behandletAvSpinnvill", true)
            }
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
            it.requireAny(
                "Godkjenning.sykepengegrunnlagsfakta.fastsatt",
                listOf("EtterHovedregel", "IInfotrygd", "EtterSkjønn"),
            )
            it.require("Godkjenning.sykepengegrunnlagsfakta.fastsatt") { fastsattNode ->
                when (fastsattNode.asText()) {
                    "EtterHovedregel" -> {
                        it.requireArray("Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere") {
                            requireKey("arbeidsgiver", "omregnetÅrsinntekt", "inntektskilde")
                        }
                    }

                    "EtterSkjønn" -> {
                        it.requireArray("Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere") {
                            requireKey("arbeidsgiver", "omregnetÅrsinntekt", "inntektskilde", "skjønnsfastsatt")
                        }
                    }

                    else -> {}
                }
            }
            it.requireArray("Godkjenning.perioderMedSammeSkjæringstidspunkt") {
                requireKey("vedtaksperiodeId", "behandlingId", "fom", "tom")
            }
            it.interestedIn("avviksvurderingId")
            it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
            it.interestedIn("Godkjenning.orgnummereMedRelevanteArbeidsforhold")
            it.requireArray("Godkjenning.omregnedeÅrsinntekter") {
                requireKey("organisasjonsnummer", "beløp")
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(
            Godkjenningsbehov(
                id = packet["@id"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
                skjæringstidspunkt = LocalDate.parse(packet["Godkjenning.skjæringstidspunkt"].asText()),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                avviksvurderingId =
                    packet["avviksvurderingId"].takeUnless { it.isMissingOrNull() }
                        ?.let { UUID.fromString(it.asText()) },
                vilkårsgrunnlagId = packet["Godkjenning.vilkårsgrunnlagId"].asUUID(),
                spleisVedtaksperioder = spleisvedtaksperioder(packet),
                spleisBehandlingId = packet["Godkjenning.behandlingId"].asUUID(),
                tags = packet["Godkjenning.tags"].map { it.asText() },
                utbetalingId = packet["utbetalingId"].asUUID(),
                periodetype = Periodetype.valueOf(packet["Godkjenning.periodetype"].asText()),
                førstegangsbehandling = packet["Godkjenning.førstegangsbehandling"].asBoolean(),
                utbetalingtype = Utbetalingtype.valueOf(packet["Godkjenning.utbetalingtype"].asText()),
                inntektskilde = Inntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
                orgnummereMedRelevanteArbeidsforhold =
                    packet["Godkjenning.orgnummereMedRelevanteArbeidsforhold"]
                        .takeUnless(JsonNode::isMissingOrNull)
                        ?.map { it.asText() } ?: emptyList(),
                spleisSykepengegrunnlagsfakta = spleisSykepengegrunnlagsfakta(packet),
                kanAvvises = packet["Godkjenning.kanAvvises"].asBoolean(),
                erInngangsvilkårVurdertISpleis = packet["Godkjenning.sykepengegrunnlagsfakta.fastsatt"].asText() != "IInfotrygd",
                omregnedeÅrsinntekter =
                    packet["Godkjenning.omregnedeÅrsinntekter"].map {
                        OmregnetÅrsinntekt(
                            arbeidsgiverreferanse = it["organisasjonsnummer"].asText(),
                            beløp = it["beløp"].asDouble(),
                        )
                    },
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }

    private fun spleisvedtaksperioder(packet: JsonMessage): List<SpleisVedtaksperiode> {
        return packet["Godkjenning.perioderMedSammeSkjæringstidspunkt"].map { periodeNode ->
            SpleisVedtaksperiode(
                vedtaksperiodeId = periodeNode["vedtaksperiodeId"].asUUID(),
                spleisBehandlingId = periodeNode["behandlingId"].asUUID(),
                fom = periodeNode["fom"].asLocalDate(),
                tom = periodeNode["tom"].asLocalDate(),
                skjæringstidspunkt = packet["Godkjenning.skjæringstidspunkt"].asLocalDate(),
            )
        }
    }

    private fun spleisSykepengegrunnlagsfakta(packet: JsonMessage): SpleisSykepengegrunnlagsfakta {
        when (packet["Godkjenning.sykepengegrunnlagsfakta.fastsatt"].asText()) {
            "IInfotrygd" -> return SpleisSykepengegrunnlagsfakta(
                arbeidsgivere = emptyList(),
            )

            else -> return SpleisSykepengegrunnlagsfakta(
                arbeidsgivere =
                    packet["Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere"].map {
                        SykepengegrunnlagsArbeidsgiver(
                            arbeidsgiver = it["arbeidsgiver"].asText(),
                            omregnetÅrsinntekt = it["omregnetÅrsinntekt"].asDouble(),
                            inntektskilde = Inntektsopplysningkilde.valueOf(it["inntektskilde"].asText()),
                            skjønnsfastsatt = it["skjønnsfastsatt"]?.asDouble(),
                        )
                    },
            )
        }
    }
}
