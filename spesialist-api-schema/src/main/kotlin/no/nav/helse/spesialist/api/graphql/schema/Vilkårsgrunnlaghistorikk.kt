package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.util.UUID

@GraphQLName("Vilkarsgrunnlagtype")
enum class ApiVilkårsgrunnlagtype { INFOTRYGD, SPLEIS, UKJENT }

@GraphQLName("Vilkarsgrunnlag")
interface ApiVilkårsgrunnlag {
    val id: UUID
    val vilkarsgrunnlagtype: ApiVilkårsgrunnlagtype
    val inntekter: List<ApiArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>
    val omregnetArsinntekt: Double
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: Double
}

@GraphQLName("VilkarsgrunnlagInfotrygd")
data class ApiVilkårsgrunnlagInfotrygd(
    override val id: UUID,
    override val vilkarsgrunnlagtype: ApiVilkårsgrunnlagtype,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
) : ApiVilkårsgrunnlag

@GraphQLName("VilkarsgrunnlagSpleis")
data class ApiVilkårsgrunnlagSpleis(
    override val id: UUID,
    override val vilkarsgrunnlagtype: ApiVilkårsgrunnlagtype,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    val sammenligningsgrunnlag: Double?,
    val skjonnsmessigFastsattAarlig: Double?,
    val avviksprosent: Double?,
    val antallOpptjeningsdagerErMinst: Int,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: ApiSykepengegrunnlagsgrense,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: LocalDate,
) : ApiVilkårsgrunnlag

@GraphQLName("Sykepengegrunnlagsgrense")
data class ApiSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
)
