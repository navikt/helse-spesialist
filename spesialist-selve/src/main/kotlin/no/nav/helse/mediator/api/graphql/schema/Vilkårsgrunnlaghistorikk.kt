package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.enums.GraphQLVilkarsgrunnlagtype
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLVilkarsgrunnlag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLVilkarsgrunnlaghistorikk

enum class Vilkarsgrunnlagtype { INFOTRYGD, SPLEIS, UKJENT }

data class Vilkarsgrunnlaghistorikk(
    val id: UUID,
    val grunnlag: List<Vilkarsgrunnlag>
)

interface Vilkarsgrunnlag {
    val vilkarsgrunnlagtype: Vilkarsgrunnlagtype
    val inntekter: List<Arbeidsgiverinntekt>
    val omregnetArsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: Double
}

data class VilkarsgrunnlagInfotrygd(
    override val vilkarsgrunnlagtype: Vilkarsgrunnlagtype,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double
) : Vilkarsgrunnlag

data class VilkarsgrunnlagSpleis(
    override val vilkarsgrunnlagtype: Vilkarsgrunnlagtype,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    val antallOpptjeningsdagerErMinst: Int,
    val grunnbelop: Int,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: LocalDate
) : Vilkarsgrunnlag

private fun GraphQLVilkarsgrunnlagtype.tilVilkarsgrunnlagtype(): Vilkarsgrunnlagtype =
    when (this) {
        GraphQLVilkarsgrunnlagtype.INFOTRYGD -> Vilkarsgrunnlagtype.INFOTRYGD
        GraphQLVilkarsgrunnlagtype.SPLEIS -> Vilkarsgrunnlagtype.SPLEIS
        GraphQLVilkarsgrunnlagtype.UKJENT -> Vilkarsgrunnlagtype.UKJENT
        else -> throw Exception("Ukjent vilkårsgrunnlagtype ${this.name}")
    }

private fun GraphQLVilkarsgrunnlag.tilVilkarsgrunnlag(): Vilkarsgrunnlag =
    when (this) {
        is GraphQLSpleisVilkarsgrunnlag -> VilkarsgrunnlagSpleis(
            vilkarsgrunnlagtype = vilkarsgrunnlagtype.tilVilkarsgrunnlagtype(),
            inntekter = inntekter.map { it.tilArbeidsgiverinntekt() },
            omregnetArsinntekt = omregnetArsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            skjaeringstidspunkt = skjaeringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            grunnbelop = grunnbelop,
            oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap,
            oppfyllerKravOmMinstelonn = oppfyllerKravOmMinstelonn,
            oppfyllerKravOmOpptjening = oppfyllerKravOmOpptjening,
            opptjeningFra = opptjeningFra
        )
        is GraphQLInfotrygdVilkarsgrunnlag -> VilkarsgrunnlagInfotrygd(
            vilkarsgrunnlagtype = vilkarsgrunnlagtype.tilVilkarsgrunnlagtype(),
            inntekter = inntekter.map { it.tilArbeidsgiverinntekt() },
            omregnetArsinntekt = omregnetArsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            skjaeringstidspunkt = skjaeringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag
        )
        else -> throw Exception("Ukjent vilkårsgrunnlag ${this.javaClass.name}")
    }

internal fun GraphQLVilkarsgrunnlaghistorikk.tilVilkarsgrunnlaghistorikk(): Vilkarsgrunnlaghistorikk =
    Vilkarsgrunnlaghistorikk(
        id = id,
        grunnlag = grunnlag.map { it.tilVilkarsgrunnlag() }
    )
