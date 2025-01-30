package no.nav.helse.sidegig

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class PgBehandlingDaoTest: AbstractDatabaseTest() {
    @Test
    fun `insert behandling`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.of(2018, 1, 1)
        val tom = LocalDate.of(2018, 1, 31)
        val opprettet = LocalDateTime.now()

        val behandling = Behandling(
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = fom,
            opprettet = opprettet,
        )
        behandlingDao.lagreBehandling(behandling)
        assertBehandling(behandling)
    }
    @Test
    fun `insert to behandlinger med samme pk medfører ikke feil`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.of(2018, 1, 1)
        val tom = LocalDate.of(2018, 1, 31)
        val opprettet = LocalDateTime.now()

        val behandling = Behandling(
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = fom,
            opprettet = opprettet,
        )
        behandlingDao.lagreBehandling(behandling)
        assertDoesNotThrow {
            behandlingDao.lagreBehandling(behandling)
        }
        assertBehandling(behandling)
    }

    private fun assertBehandling(forventetBehandling: Behandling) {
        @Language("PostgreSQL")
        val query = "SELECT * FROM behandling_v2 WHERE behandling_id = :behandling_id"
        val behandling = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("behandling_id" to forventetBehandling.behandlingId)).map {
                    Behandling(
                        vedtaksperiodeId = it.uuid("vedtaksperiode_id"),
                        behandlingId = it.uuid("behandling_id"),
                        fom = it.localDate("fom"),
                        tom = it.localDate("tom"),
                        skjæringstidspunkt = it.localDate("skjæringstidspunkt"),
                        opprettet = it.localDateTime("opprettet"),
                    )
                }.asSingle
            )
        }

        assertNotNull(behandling)
        assertEquals(behandling.vedtaksperiodeId, forventetBehandling.vedtaksperiodeId)
        assertEquals(behandling.behandlingId, forventetBehandling.behandlingId)
        assertEquals(behandling.fom, forventetBehandling.fom)
        assertEquals(behandling.tom, forventetBehandling.tom)
        assertEquals(behandling.skjæringstidspunkt, forventetBehandling.skjæringstidspunkt)
        assertEquals(behandling.opprettet.withNano(0), forventetBehandling.opprettet.withNano(0))
    }
}
