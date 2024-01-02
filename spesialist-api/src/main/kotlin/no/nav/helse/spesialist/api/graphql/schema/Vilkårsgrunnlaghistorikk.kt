package no.nav.helse.spesialist.api.graphql.schema

import java.util.UUID
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVilkarsgrunnlag

enum class Vilkarsgrunnlagtype { INFOTRYGD, SPLEIS, UKJENT }

interface Vilkarsgrunnlag {
    val id: UUIDString
    val vilkarsgrunnlagtype: Vilkarsgrunnlagtype
    val inntekter: List<Arbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>
    val omregnetArsinntekt: Double
    val skjaeringstidspunkt: DateString
    val sykepengegrunnlag: Double
}

data class VilkarsgrunnlagInfotrygd(
    override val id: UUIDString,
    override val vilkarsgrunnlagtype: Vilkarsgrunnlagtype,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: DateString,
    override val sykepengegrunnlag: Double,
) : Vilkarsgrunnlag

data class VilkarsgrunnlagSpleis(
    override val id: UUIDString,
    override val vilkarsgrunnlagtype: Vilkarsgrunnlagtype,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: DateString,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    val sammenligningsgrunnlag: Double?,
    val skjonnsmessigFastsattAarlig: Double?,
    val avviksprosent: Double?,
    val antallOpptjeningsdagerErMinst: Int,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: Sykepengegrunnlagsgrense,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: DateString,
) : Vilkarsgrunnlag

internal fun GraphQLVilkarsgrunnlag.tilVilkarsgrunnlag(avviksvurderinghenter: Avviksvurderinghenter): Vilkarsgrunnlag {
    return when (this) {
        is GraphQLSpleisVilkarsgrunnlag -> {
            val avviksvurdering = avviksvurderinghenter.hentAvviksvurdering(UUID.fromString(id))
            if (avviksvurdering == null) {
                throw IllegalStateException("Avviksvurdering null for vilkårsgrunnlag $id")
            }
            VilkarsgrunnlagSpleis(
                inntekter = inntekter.map { arbeidsgiverinntekt ->
                    val arbeidsgiverinntekter =
                        avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.singleOrNull {
                            it.arbeidsgiverreferanse == arbeidsgiverinntekt.arbeidsgiver
                        }?.inntekter ?: emptyList()
                    Arbeidsgiverinntekt(
                        arbeidsgiver = arbeidsgiverinntekt.arbeidsgiver,
                        omregnetArsinntekt = arbeidsgiverinntekt.omregnetArsinntekt.tilOmregnetÅrsinntekt(),
                        sammenligningsgrunnlag = Sammenligningsgrunnlag(
                            belop = arbeidsgiverinntekter.sumOf { it.beløp },
                            inntektFraAOrdningen = arbeidsgiverinntekter.map { inntekt ->
                                InntektFraAOrdningen(
                                    maned = inntekt.årMåned.toString(),
                                    sum = inntekt.beløp
                                )
                            }
                        ),
                        skjonnsmessigFastsatt = arbeidsgiverinntekt.skjonnsmessigFastsatt?.tilOmregnetÅrsinntekt(),
                        deaktivert = arbeidsgiverinntekt.deaktivert
                    )
                },
                omregnetArsinntekt = avviksvurdering.beregningsgrunnlag.totalbeløp,
                sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag.totalbeløp,
                avviksprosent = avviksvurdering.avviksprosent,
                vilkarsgrunnlagtype = Vilkarsgrunnlagtype.SPLEIS,
                id = id,
                arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilArbeidsgiverrefusjon() },
                skjonnsmessigFastsattAarlig = skjonnsmessigFastsattAarlig,
                skjaeringstidspunkt = skjaeringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag,
                antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
                grunnbelop = grunnbelop,
                sykepengegrunnlagsgrense = sykepengegrunnlagsgrense.tilSykepengegrunnlaggrense(),
                oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap,
                oppfyllerKravOmMinstelonn = oppfyllerKravOmMinstelonn,
                oppfyllerKravOmOpptjening = oppfyllerKravOmOpptjening,
                opptjeningFra = opptjeningFra
            )
        }

        is GraphQLInfotrygdVilkarsgrunnlag -> VilkarsgrunnlagInfotrygd(
            id = id,
            vilkarsgrunnlagtype = Vilkarsgrunnlagtype.INFOTRYGD,
            inntekter = inntekter.map {
                Arbeidsgiverinntekt(
                    arbeidsgiver = it.arbeidsgiver,
                    omregnetArsinntekt = it.omregnetArsinntekt?.tilOmregnetÅrsinntekt(),
                    sammenligningsgrunnlag = null,
                    skjonnsmessigFastsatt = null,
                    deaktivert = it.deaktivert
                )
            },
            arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilArbeidsgiverrefusjon() },
            omregnetArsinntekt = omregnetArsinntekt,
            skjaeringstidspunkt = skjaeringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag
        )

        else -> throw Exception("Ukjent vilkårsgrunnlag ${this.javaClass.name}")
    }
}

internal fun GraphQLSykepengegrunnlagsgrense.tilSykepengegrunnlaggrense() =
    Sykepengegrunnlagsgrense(grunnbelop, grense, virkningstidspunkt)

data class Sykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: DateString,
)