package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage

internal class AvsluttetMedVedtakRiver(
    private val mediator: MeldingMediator,
    private val avviksvurderingDao: AvviksvurderingDao,
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
            it.requireKey(
                "sykepengegrunnlag",
                "grunnlagForSykepengegrunnlag",
                "grunnlagForSykepengegrunnlagPerArbeidsgiver",
            )
            it.requireKey("begrensning", "inntekt", "vedtakFattetTidspunkt")
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
        mediator.mottaMelding(AvsluttetMedVedtakMessage(packet, avviksvurderingDao), context)
    }
}
