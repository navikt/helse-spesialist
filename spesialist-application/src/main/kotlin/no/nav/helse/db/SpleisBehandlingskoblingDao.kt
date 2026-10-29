package no.nav.helse.db

import java.time.LocalDateTime
import java.util.UUID

// Midlertidig: brukes bare til engangsmigreringen som gir varsler spleis_behandling_id
interface SpleisBehandlingskoblingDao {
    fun finnBehandlinger(vedtaksperiodeId: UUID): List<SpesialistBehandling>

    fun antallVarslerUtenSpleisBehandlingId(behandlingRef: Long): Int

    fun settSpleisBehandlingIdPåVarsler(
        behandlingRef: Long,
        spleisBehandlingId: UUID,
    ): Int

    data class SpesialistBehandling(
        val id: Long,
        val utbetalingId: UUID?,
        val spleisBehandlingId: UUID?,
        val opprettet: LocalDateTime,
    )
}
