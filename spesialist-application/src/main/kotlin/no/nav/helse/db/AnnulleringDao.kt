package no.nav.helse.db

import java.time.LocalDate
import java.util.UUID

interface AnnulleringDao {
    fun find10Annulleringer(): List<AnnullertAvSaksbehandlerRow>

    fun findUtbetalingId(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): UUID?

    fun finnBehandlingISykefraværstilfelle(utbetalingId: UUID): BehandlingISykefraværstilfelleRow?

    fun finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandlingISykefraværstilfelleRow: BehandlingISykefraværstilfelleRow): UUID?

    fun oppdaterAnnulleringMedVedtaksperiodeId(
        annulleringId: Int,
        vedtaksperiodeId: UUID,
    ): Int
}

data class BehandlingsperiodeRow(
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingId: UUID?,
)

data class AnnullertAvSaksbehandlerRow(
    val id: Int,
    val arbeidsgiver_fagsystem_id: String,
    val person_fagsystem_id: String,
)

data class BehandlingISykefraværstilfelleRow(
    val behandlingId: Long,
    val vedtaksperiodeId: UUID,
    val skjæringstidspunkt: LocalDate,
    val personId: Long,
    val arbeidsgiverId: String,
    val utbetalingId: UUID,
)
