package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed interface SnapshotTidslinjeperiode {
    val behandlingId: UUID
    val erForkastet: Boolean
    val fom: LocalDate
    val tom: LocalDate
    val inntektstype: SnapshotInntektstype
    val opprettet: LocalDateTime
    val periodetype: SnapshotPeriodetype
    val periodetilstand: SnapshotPeriodetilstand
    val skjaeringstidspunkt: LocalDate
    val tidslinje: List<SnapshotDag>
    val hendelser: List<SnapshotHendelse>
    val vedtaksperiodeId: UUID
}

data class SnapshotUberegnetPeriode(
    override val behandlingId: UUID,
    override val erForkastet: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val inntektstype: SnapshotInntektstype,
    override val opprettet: LocalDateTime,
    override val periodetype: SnapshotPeriodetype,
    override val periodetilstand: SnapshotPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val tidslinje: List<SnapshotDag>,
    override val hendelser: List<SnapshotHendelse>,
    override val vedtaksperiodeId: UUID,
) : SnapshotTidslinjeperiode

data class SnapshotBeregnetPeriode(
    override val behandlingId: UUID,
    override val erForkastet: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val inntektstype: SnapshotInntektstype,
    override val opprettet: LocalDateTime,
    override val periodetype: SnapshotPeriodetype,
    override val periodetilstand: SnapshotPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val tidslinje: List<SnapshotDag>,
    override val hendelser: List<SnapshotHendelse>,
    override val vedtaksperiodeId: UUID,
    val forbrukteSykedager: Int?,
    val gjenstaendeSykedager: Int?,
    val maksdato: LocalDate,
    val periodevilkar: SnapshotPeriodevilkar,
    val utbetaling: SnapshotUtbetaling,
    val vilkarsgrunnlagId: UUID?,
    val pensjonsgivendeInntekter: List<SnapshotPensjonsgivendeInntekt>,
    val annulleringskandidater: List<SnapshotAnnulleringskandidat>,
) : SnapshotTidslinjeperiode

data class SnapshotUkjentTidslinjeperiode(
    override val behandlingId: UUID,
    override val erForkastet: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val inntektstype: SnapshotInntektstype,
    override val opprettet: LocalDateTime,
    override val periodetype: SnapshotPeriodetype,
    override val periodetilstand: SnapshotPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val tidslinje: List<SnapshotDag>,
    override val hendelser: List<SnapshotHendelse>,
    override val vedtaksperiodeId: UUID,
) : SnapshotTidslinjeperiode
