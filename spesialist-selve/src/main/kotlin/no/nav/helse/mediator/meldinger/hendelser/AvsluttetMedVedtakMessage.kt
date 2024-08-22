package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtak.Faktatype
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetMedVedtak
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.gjenopprett
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntektDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.util.UUID

internal class AvsluttetMedVedtakMessage(
    private val packet: JsonMessage,
    private val avviksvurderingDao: AvviksvurderingDao,
) : Vedtaksperiodemelding {
    private val fødselsnummer = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val vedtakFattetTidspunkt = packet["vedtakFattetTidspunkt"].asLocalDateTime()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    private val spleisBehandlingId = UUID.fromString(packet["behandlingId"].asText())
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val hendelser = packet["hendelser"].map { it.asUUID() }
    private val sykepengegrunnlag = packet["sykepengegrunnlag"].asDouble()
    private val grunnlagForSykepengegrunnlag = packet["grunnlagForSykepengegrunnlag"].asDouble()
    private val grunnlagForSykepengegrunnlagPerArbeidsgiver =
        jacksonObjectMapper().treeToValue<Map<String, Double>>(
            packet["grunnlagForSykepengegrunnlagPerArbeidsgiver"],
        )
    private val begrensning = packet["begrensning"].asText()
    private val inntekt = packet["inntekt"].asDouble()
    private val sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(packet, faktatype(packet))

    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        person.fattVedtak(avsluttetMedVedtak)
    }

    override val id: UUID = packet["@id"].asUUID()

    override fun toJson(): String = packet.toJson()

    private val avsluttetMedVedtak get() =
        AvsluttetMedVedtak(
            spleisBehandlingId = spleisBehandlingId,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

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

        val avviksvurderingDto = finnAvviksvurdering().toDto()
        val innrapportertÅrsinntekt = avviksvurderingDto.sammenligningsgrunnlag.totalbeløp
        val avviksprosent = avviksvurderingDto.avviksprosent
        val innrapporterteInntekter = avviksvurderingDto.sammenligningsgrunnlag.innrapporterteInntekter

        return when (faktatype) {
            Faktatype.ETTER_SKJØNN ->
                Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                    omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                    innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                    avviksprosent = avviksprosent,
                    seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                    skjønnsfastsatt = packet["sykepengegrunnlagsfakta.skjønnsfastsatt"].asDouble(),
                    tags = mutableSetOf(),
                    arbeidsgivere =
                        packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                innrapportertÅrsinntekt = innrapporterteInntekter(organisasjonsnummer, innrapporterteInntekter),
                                skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asDouble(),
                            )
                        },
                )

            Faktatype.ETTER_HOVEDREGEL ->
                Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                    omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                    innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                    avviksprosent = avviksprosent,
                    seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                    tags = mutableSetOf(),
                    arbeidsgivere =
                        packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
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

    private fun finnAvviksvurdering() =
        checkNotNull(
            avviksvurderingDao.finnAvviksvurderinger(fødselsnummer).gjenopprett().finnRiktigAvviksvurdering(skjæringstidspunkt),
        ) {
            "Forventet å finne avviksvurdering for $aktørId og skjæringstidspunkt $skjæringstidspunkt"
        }

    private fun innrapporterteInntekter(
        arbeidsgiverreferanse: String,
        innrapportertInntekter: List<InnrapportertInntektDto>,
    ): Double =
        innrapportertInntekter
            .filter { it.arbeidsgiverreferanse == arbeidsgiverreferanse }
            .flatMap { it.inntekter }
            .sumOf { it.beløp }
}
