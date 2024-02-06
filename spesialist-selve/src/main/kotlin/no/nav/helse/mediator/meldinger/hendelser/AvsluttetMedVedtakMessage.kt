package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.modell.avviksvurdering.InnrapportertInntektDto
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetMedVedtak
import no.nav.helse.modell.vedtaksperiode.vedtak.Faktatype
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.LoggerFactory

internal class AvsluttetMedVedtakMessage(packet: JsonMessage, private val avviksvurderingDao: AvviksvurderingDao) {

    private val fødselsnummer = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val fom = packet["fom"].asLocalDate()
    private val tom = packet["tom"].asLocalDate()
    private val vedtakFattetTidspunkt = packet["vedtakFattetTidspunkt"].asLocalDateTime()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val utbetalingId = packet["utbetalingId"].asUUID()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val hendelser = packet["hendelser"].map { it.asUUID() }
    private val sykepengegrunnlag = packet["sykepengegrunnlag"].asDouble()
    private val grunnlagForSykepengegrunnlag = packet["grunnlagForSykepengegrunnlag"].asDouble()
    private val grunnlagForSykepengegrunnlagPerArbeidsgiver = jacksonObjectMapper().treeToValue<Map<String, Double>>(packet["grunnlagForSykepengegrunnlagPerArbeidsgiver"])
    private val begrensning = packet["begrensning"].asText()
    private val inntekt = packet["inntekt"].asDouble()
    private val tags = packet["tags"].map { it.asText() }
    private val sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(packet, faktatype(packet))
    internal fun skjæringstidspunkt() = skjæringstidspunkt
    internal fun fødselsnummer() = fødselsnummer

    private val avsluttetMedVedtak get() = AvsluttetMedVedtak(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        utbetalingId = utbetalingId,
        skjæringstidspunkt = skjæringstidspunkt,
        hendelser = hendelser,
        sykepengegrunnlag = sykepengegrunnlag,
        grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning = begrensning,
        inntekt = inntekt,
        sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
        fom = fom,
        tom = tom,
        vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        tags = tags
    )

    internal fun sendInnTil(sykefraværstilfelle: Sykefraværstilfelle) {
        sykefraværstilfelle.håndter(avsluttetMedVedtak)
    }

    private fun faktatype(packet: JsonMessage): Faktatype {
        return when (val fastsattString = packet["sykepengegrunnlagsfakta.fastsatt"].asText()) {
            "EtterSkjønn" -> Faktatype.ETTER_SKJØNN
            "EtterHovedregel" -> Faktatype.ETTER_HOVEDREGEL
            "IInfotrygd" -> Faktatype.I_INFOTRYGD
            else -> throw IllegalArgumentException("FastsattType $fastsattString er ikke støttet")
        }
    }

    private fun sykepengegrunnlagsfakta(packet: JsonMessage, faktatype: Faktatype): Sykepengegrunnlagsfakta {
        if (faktatype == Faktatype.I_INFOTRYGD) return Sykepengegrunnlagsfakta.Infotrygd(
            omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
        )

        val avviksvurdering: Avviksvurdering? = avviksvurderingDao.finnAvviksvurderinger(fødselsnummer).finnRiktigAvviksvurdering(skjæringstidspunkt)

        logger.info(
            "Bruker avviksvurdering fra: ${if (avviksvurdering == null) "spleis" else "spinnvill"} for {}",
            kv("hendelseId", packet["@id"])
        )

        val avviksvurderingDto = avviksvurdering?.toDto()
        val innrapportertÅrsinntekt = avviksvurderingDto?.sammenligningsgrunnlag?.totalbeløp
            ?: packet["sykepengegrunnlagsfakta.innrapportertÅrsinntekt"].let { if (it.isMissingOrNull()) throw IllegalStateException() else it }
                .asDouble()
        val avviksprosent = avviksvurderingDto?.avviksprosent
            ?: packet["sykepengegrunnlagsfakta.avviksprosent"].takeIf { it.isNumber }?.doubleValue()
            ?: error("Her mangler det BÅDE spinnvill avviksvurdering OG informasjon fra spleis 😱")
        val innrapporterteInntekter = avviksvurderingDto?.sammenligningsgrunnlag?.innrapporterteInntekter ?: error("Avviksvurdering mangler")

        return when (faktatype) {
            Faktatype.ETTER_SKJØNN -> Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                avviksprosent = avviksprosent,
                seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                skjønnsfastsatt = packet["sykepengegrunnlagsfakta.skjønnsfastsatt"].asDouble(),
                tags = packet["sykepengegrunnlagsfakta.tags"].map { it.asText() },
                arbeidsgivere = packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                    val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                        organisasjonsnummer = organisasjonsnummer,
                        omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                        innrapportertÅrsinntekt = innrapporterteInntekter(organisasjonsnummer, innrapporterteInntekter),
                        skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asDouble(),
                    )
                },
            )

            Faktatype.ETTER_HOVEDREGEL -> Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                avviksprosent = avviksprosent,
                seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                tags = packet["sykepengegrunnlagsfakta.tags"].map { it.asText() },
                arbeidsgivere = packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                    val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                        organisasjonsnummer = organisasjonsnummer,
                        omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                        innrapportertÅrsinntekt = innrapporterteInntekter(organisasjonsnummer, innrapporterteInntekter),
                    )
                },
            )

            else -> error("Her vet vi ikke hva som har skjedd. Feil i kompilatoren?")
        }
    }

    private fun innrapporterteInntekter(
        arbeidsgiverreferanse: String,
        innrapportertInntekter: List<InnrapportertInntektDto>,
    ): Double =
        innrapportertInntekter
            .filter { it.arbeidsgiverreferanse == arbeidsgiverreferanse }
            .flatMap { it.inntekter }
            .sumOf { it.beløp }

    companion object {
        private val logger = LoggerFactory.getLogger(AvsluttetMedVedtakMessage::class.java)
    }
}
