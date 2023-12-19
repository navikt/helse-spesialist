package no.nav.helse.mediator.meldinger

import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.hendelser.UtkastTilVedtakMessage
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class UtkastTilVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
    private val avviksvurderingDao: AvviksvurderingDao,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny("@event_name", listOf("utkast_til_vedtak", "avsluttet_med_vedtak"))
                it.requireKey("@id", "fødselsnummer", "aktørId", "vedtaksperiodeId", "organisasjonsnummer")
                it.requireKey("fom", "tom", "skjæringstidspunkt")
                it.requireArray("hendelser")
                it.requireKey("sykepengegrunnlag", "grunnlagForSykepengegrunnlag", "grunnlagForSykepengegrunnlagPerArbeidsgiver")
                it.requireKey("begrensning", "inntekt", "vedtakFattetTidspunkt", "tags")

                it.interestedIn(
                    "sykepengegrunnlagsfakta",
                    "sykepengegrunnlagsfakta.fastsatt",
                    "sykepengegrunnlagsfakta.omregnetÅrsinntekt",
                    "sykepengegrunnlagsfakta.innrapportertÅrsinntekt",
                    "sykepengegrunnlagsfakta.avviksprosent",
                    "sykepengegrunnlagsfakta.6G",
                    "sykepengegrunnlagsfakta.skjønnsfastsatt",
                    "sykepengegrunnlagsfakta.tags",
                    "sykepengegrunnlagsfakta.arbeidsgivere",
                )
                it.interestedIn("utbetalingId")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.error("Forstod ikke utkast_til_vedtak:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info("Mottok melding om utkast_til_vedtak:\n${packet.toJson()}")
        mediator.håndter(UtkastTilVedtakMessage(packet, avviksvurderingDao))
    }

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }
}
