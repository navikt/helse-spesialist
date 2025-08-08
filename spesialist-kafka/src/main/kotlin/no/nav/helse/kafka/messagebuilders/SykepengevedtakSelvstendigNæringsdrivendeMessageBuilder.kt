package no.nav.helse.kafka.messagebuilders

import no.nav.helse.modell.melding.SykepengevedtakSelvstendigNæringsdrivendeDto
import no.nav.helse.modell.vedtak.Utfall

internal fun SykepengevedtakSelvstendigNæringsdrivendeDto.detaljer(): Map<String, Any> =
    mapOf(
        "yrkesaktivitetstype" to "SELVSTENDIG",
        "fødselsnummer" to fødselsnummer,
        "vedtaksperiodeId" to "$vedtaksperiodeId",
        "organisasjonsnummer" to "SELVSTENDIG",
        "fom" to "$fom",
        "tom" to "$tom",
        "skjæringstidspunkt" to "$skjæringstidspunkt",
        "hendelser" to hendelser,
        "sykepengegrunnlag" to sykepengegrunnlag,
        "vedtakFattetTidspunkt" to "$vedtakFattetTidspunkt",
        "utbetalingId" to "$utbetalingId",
        "sykepengegrunnlagsfakta" to
            mapOf(
                "beregningsgrunnlag" to sykepengegrunnlagsfakta.beregningsgrunnlag,
                "pensjonsgivendeInntekter" to
                    sykepengegrunnlagsfakta.pensjonsgivendeInntekter.map { inntekt ->
                        mapOf(
                            "år" to inntekt.år,
                            "inntekt" to inntekt.inntekt,
                        )
                    },
                "erBegrensetTil6G" to sykepengegrunnlagsfakta.erBegrensetTil6G,
                "6G" to sykepengegrunnlagsfakta.`6G`,
            ),
        "begrunnelser" to (
            vedtakBegrunnelse?.let {
                listOf(
                    mapOf(
                        "type" to
                            when (it.utfall) {
                                Utfall.AVSLAG -> "Avslag"
                                Utfall.DELVIS_INNVILGELSE -> "DelvisInnvilgelse"
                                Utfall.INNVILGELSE -> "Innvilgelse"
                            },
                        "begrunnelse" to (it.begrunnelse ?: ""),
                        "perioder" to
                            listOf(
                                mapOf(
                                    "fom" to "$fom",
                                    "tom" to "$tom",
                                ),
                            ),
                    ),
                )
            } ?: emptyList()
        ),
    )
