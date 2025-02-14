package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.util.UUID

sealed interface SnapshotVilkarsgrunnlag {
    val id: UUID
    val inntekter: List<SnapshotArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<SnapshotArbeidsgiverrefusjon>
    val omregnetArsinntekt: Double
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: Double
}

data class SnapshotInfotrygdVilkarsgrunnlag(
    override val id: UUID,
    override val inntekter: List<SnapshotArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<SnapshotArbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
) : SnapshotVilkarsgrunnlag

data class SnapshotSpleisVilkarsgrunnlag(
    override val id: UUID,
    override val inntekter: List<SnapshotArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<SnapshotArbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    val antallOpptjeningsdagerErMinst: Int,
    val skjonnsmessigFastsattAarlig: Double?,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: SnapshotSykepengegrunnlagsgrense,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: LocalDate,
) : SnapshotVilkarsgrunnlag

data class SnapshotUkjentVilkarsgrunnlag(
    override val id: UUID,
    override val inntekter: List<SnapshotArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<SnapshotArbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
) : SnapshotVilkarsgrunnlag
