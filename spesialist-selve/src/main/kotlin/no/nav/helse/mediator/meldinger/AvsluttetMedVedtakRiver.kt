package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AvsluttetMedVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
    private val avviksvurderingDao: AvviksvurderingDao,
    private val generasjonDao: GenerasjonDao,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "avsluttet_med_vedtak")
                it.requireKey("@id", "fødselsnummer", "aktørId", "vedtaksperiodeId", "organisasjonsnummer")
                it.requireKey("fom", "tom", "skjæringstidspunkt")
                it.requireArray("hendelser")
                it.requireKey(
                    "sykepengegrunnlag",
                    "grunnlagForSykepengegrunnlag",
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver"
                )
                it.requireKey("begrensning", "inntekt", "vedtakFattetTidspunkt", "tags")
                it.requireKey("utbetalingId", "behandlingId")

                it.requireAny(
                    "sykepengegrunnlagsfakta.fastsatt",
                    listOf("EtterHovedregel", "IInfotrygd", "EtterSkjønn")
                )
                it.requireKey("sykepengegrunnlagsfakta.omregnetÅrsinntekt")
                it.require("sykepengegrunnlagsfakta.fastsatt") { fastsattNode ->
                    when (fastsattNode.asText()) {
                        "EtterHovedregel" -> {
                            it.requireKey(
                                "sykepengegrunnlagsfakta.6G",
                                "sykepengegrunnlagsfakta.tags",
                                "sykepengegrunnlagsfakta.arbeidsgivere",
                            )
                        }

                        "EtterSkjønn" -> {
                            it.requireKey(
                                "sykepengegrunnlagsfakta.6G",
                                "sykepengegrunnlagsfakta.tags",
                                "sykepengegrunnlagsfakta.arbeidsgivere",
                                "sykepengegrunnlagsfakta.skjønnsfastsatt",
                            )
                        }

                        else -> {}
                    }
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.error("Forstod ikke avsluttet_med_vedtak:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info("Mottok melding avsluttet_med_vedtak:\n${packet.toJson()}")
        if (packet["@id"].asText() in listOf(
                "7aa38978-beb6-4c92-8c4d-97aef476dc1f",
                "74e9b587-ba93-44e7-a0b7-24bacc313a94",
                "21634a26-1500-4bcd-9cb9-02bdc8f38f50",
                "cfd2fcea-d61c-4a96-a6fa-434112214291",
            )
        ) {
            sikkerlogg.info(
                "Disse må følges opp manuelt, fordi spesialist ikke videresender vedtak_fattet",
                StructuredArguments.kv("fødselsnummer", packet["fødselsnummer"]),
                StructuredArguments.kv("vedtaksperiodeId", packet["vedtaksperiodeId"]),
                StructuredArguments.kv("fom", packet["fom"]),
                StructuredArguments.kv("tom", packet["tom"]),
            )
            return
        }
        mediator.håndter(AvsluttetMedVedtakMessage(packet, avviksvurderingDao, generasjonDao), context)
    }

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }
}
