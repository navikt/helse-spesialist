package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import no.nav.helse.modell.vedtak.Faktatype
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta

class AvsluttetMedVedtakRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "avsluttet_med_vedtak")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "organisasjonsnummer")
            it.requireKey("fom", "tom", "skjæringstidspunkt")
            it.requireArray("hendelser")
            it.requireKey("sykepengegrunnlag")
            it.requireKey("vedtakFattetTidspunkt")
            it.requireKey("utbetalingId", "behandlingId")

            it.requireAny(
                "sykepengegrunnlagsfakta.fastsatt",
                listOf("EtterHovedregel", "IInfotrygd", "EtterSkjønn"),
            )
            it.requireKey("sykepengegrunnlagsfakta.omregnetÅrsinntekt")
            it.require("sykepengegrunnlagsfakta.fastsatt") { fastsattNode ->
                when (fastsattNode.asText()) {
                    "EtterHovedregel" -> {
                        it.requireKey(
                            "sykepengegrunnlagsfakta.6G",
                            "sykepengegrunnlagsfakta.arbeidsgivere",
                        )
                    }

                    "EtterSkjønn" -> {
                        it.requireKey(
                            "sykepengegrunnlagsfakta.6G",
                            "sykepengegrunnlagsfakta.arbeidsgivere",
                            "sykepengegrunnlagsfakta.skjønnsfastsatt",
                        )
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
            melding =
                AvsluttetMedVedtakMessage(
                    id = packet["@id"].asUUID(),
                    fødselsnummer = packet["fødselsnummer"].asText(),
                    vedtakFattetTidspunkt = packet["vedtakFattetTidspunkt"].asLocalDateTime(),
                    vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                    spleisBehandlingId = packet["behandlingId"].asUUID(),
                    hendelser = packet["hendelser"].map { it.asUUID() },
                    sykepengegrunnlag = packet["sykepengegrunnlag"].asDouble(),
                    sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(packet, faktatype(packet)),
                    json = packet.toJson(),
                ),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }

    private fun faktatype(packet: JsonMessage): Faktatype {
        return when (val fastsattString = packet["sykepengegrunnlagsfakta.fastsatt"].asText()) {
            "EtterSkjønn" -> Faktatype.ETTER_SKJØNN
            "EtterHovedregel" -> Faktatype.ETTER_HOVEDREGEL
            "IInfotrygd" -> Faktatype.I_INFOTRYGD
            else -> throw IllegalArgumentException("FastsattType $fastsattString er ikke støttet")
        }
    }

    private fun sykepengegrunnlagsfakta(
        packet: JsonMessage,
        faktatype: Faktatype,
    ): Sykepengegrunnlagsfakta {
        if (faktatype == Faktatype.I_INFOTRYGD) {
            return Sykepengegrunnlagsfakta.Infotrygd(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
            )
        }

        return when (faktatype) {
            Faktatype.ETTER_SKJØNN ->
                Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                    omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                    seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                    skjønnsfastsatt = packet["sykepengegrunnlagsfakta.skjønnsfastsatt"].asDouble(),
                    tags = mutableSetOf(),
                    arbeidsgivere =
                        packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asDouble(),
                            )
                        },
                )

            Faktatype.ETTER_HOVEDREGEL ->
                Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                    omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                    seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                    tags = mutableSetOf(),
                    arbeidsgivere =
                        packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                            )
                        },
                )

            else -> error("Her vet vi ikke hva som har skjedd. Feil i kompilatoren?")
        }
    }
}
