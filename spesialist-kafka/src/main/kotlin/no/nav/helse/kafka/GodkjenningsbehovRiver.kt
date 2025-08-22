package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtak.Faktatype
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import java.time.LocalDate

class GodkjenningsbehovRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAll("@behov", listOf("Godkjenning"))
            it.forbid("@løsning")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "fødselsnummer",
                "organisasjonsnummer",
                "yrkesaktivitetstype",
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
            it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
            it.interestedIn("Godkjenning.orgnummereMedRelevanteArbeidsforhold")
            it.requireArray("Godkjenning.omregnedeÅrsinntekter") {
                requireKey("organisasjonsnummer", "beløp")
            }
            it.requireAny(
                "Godkjenning.sykepengegrunnlagsfakta.fastsatt",
                listOf("EtterHovedregel", "IInfotrygd", "EtterSkjønn"),
            )
            it.requireKey("Godkjenning.sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt")
            it.require("Godkjenning.sykepengegrunnlagsfakta.fastsatt") { fastsattNode ->
                when (fastsattNode.asText()) {
                    "EtterHovedregel" -> {
                        it.requireKey(
                            "Godkjenning.sykepengegrunnlagsfakta.6G",
                            "Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere",
                            "Godkjenning.sykepengegrunnlagsfakta.sykepengegrunnlag",
                        )

                        it.requireArray("Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere") {
                            requireKey("arbeidsgiver", "omregnetÅrsinntekt", "inntektskilde")
                        }
                    }

                    "EtterSkjønn" -> {
                        it.requireKey(
                            "Godkjenning.sykepengegrunnlagsfakta.6G",
                            "Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere",
                            "Godkjenning.sykepengegrunnlagsfakta.skjønnsfastsatt",
                        )

                        it.requireArray("Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere") {
                            requireKey("arbeidsgiver", "omregnetÅrsinntekt", "skjønnsfastsatt", "inntektskilde")
                        }
                    }

                    else -> {}
                }
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
                yrkesaktivitetstype = Yrkesaktivitetstype.valueOf(packet["yrkesaktivitetstype"].asText()),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                spleisVedtaksperioder = spleisvedtaksperioder(packet),
                utbetalingId = packet["utbetalingId"].asUUID(),
                spleisBehandlingId = packet["Godkjenning.behandlingId"].asUUID(),
                vilkårsgrunnlagId = packet["Godkjenning.vilkårsgrunnlagId"].asUUID(),
                tags = packet["Godkjenning.tags"].map { it.asText() },
                periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
                periodetype = Periodetype.valueOf(packet["Godkjenning.periodetype"].asText()),
                førstegangsbehandling = packet["Godkjenning.førstegangsbehandling"].asBoolean(),
                utbetalingtype = Utbetalingtype.valueOf(packet["Godkjenning.utbetalingtype"].asText()),
                kanAvvises = packet["Godkjenning.kanAvvises"].asBoolean(),
                inntektskilde = Inntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
                orgnummereMedRelevanteArbeidsforhold =
                    packet["Godkjenning.orgnummereMedRelevanteArbeidsforhold"]
                        .takeUnless(JsonNode::isMissingOrNull)
                        ?.map { it.asText() } ?: emptyList(),
                skjæringstidspunkt = LocalDate.parse(packet["Godkjenning.skjæringstidspunkt"].asText()),
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(packet, faktatype(packet)),
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

    private fun spleisvedtaksperioder(packet: JsonMessage): List<SpleisVedtaksperiode> =
        packet["Godkjenning.perioderMedSammeSkjæringstidspunkt"].map { periodeNode ->
            SpleisVedtaksperiode(
                vedtaksperiodeId = periodeNode["vedtaksperiodeId"].asUUID(),
                spleisBehandlingId = periodeNode["behandlingId"].asUUID(),
                fom = periodeNode["fom"].asLocalDate(),
                tom = periodeNode["tom"].asLocalDate(),
                skjæringstidspunkt = packet["Godkjenning.skjæringstidspunkt"].asLocalDate(),
            )
        }

    private fun faktatype(packet: JsonMessage): Faktatype =
        when (val fastsattString = packet["Godkjenning.sykepengegrunnlagsfakta.fastsatt"].asText()) {
            "EtterSkjønn" -> Faktatype.ETTER_SKJØNN
            "EtterHovedregel" -> Faktatype.ETTER_HOVEDREGEL
            "IInfotrygd" -> Faktatype.I_INFOTRYGD
            else -> throw IllegalArgumentException("FastsattType $fastsattString er ikke støttet")
        }

    private fun sykepengegrunnlagsfakta(
        packet: JsonMessage,
        faktatype: Faktatype,
    ): Sykepengegrunnlagsfakta {
        if (faktatype == Faktatype.I_INFOTRYGD) {
            return Sykepengegrunnlagsfakta.Infotrygd(
                omregnetÅrsinntekt = packet["Godkjenning.sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt"].asDouble(),
            )
        }

        return when (faktatype) {
            Faktatype.ETTER_SKJØNN ->
                Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                    omregnetÅrsinntekt = packet["Godkjenning.sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt"].asDouble(),
                    seksG = packet["Godkjenning.sykepengegrunnlagsfakta.6G"].asDouble(),
                    skjønnsfastsatt = packet["Godkjenning.sykepengegrunnlagsfakta.skjønnsfastsatt"].asDouble(),
                    arbeidsgivere =
                        packet["Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asDouble(),
                                inntektskilde = inntektskilde(arbeidsgiver["inntektskilde"]),
                            )
                        },
                )

            Faktatype.ETTER_HOVEDREGEL ->
                Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                    omregnetÅrsinntekt = packet["Godkjenning.sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt"].asDouble(),
                    seksG = packet["Godkjenning.sykepengegrunnlagsfakta.6G"].asDouble(),
                    sykepengegrunnlag = packet["Godkjenning.sykepengegrunnlagsfakta.sykepengegrunnlag"].asDouble(),
                    arbeidsgivere =
                        packet["Godkjenning.sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                inntektskilde = inntektskilde(arbeidsgiver["inntektskilde"]),
                            )
                        },
                )

            else -> error("Her vet vi ikke hva som har skjedd. Feil i kompilatoren?")
        }
    }

    private fun inntektskilde(inntektskildeNode: JsonNode): Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde =
        when (val inntektskildeString = inntektskildeNode.asText()) {
            "Arbeidsgiver" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver
            "AOrdningen" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.AOrdningen
            "Saksbehandler" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Saksbehandler
            "Sigrun" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Sigrun
            else -> error("$inntektskildeString er ikke en gyldig inntektskilde")
        }
}
