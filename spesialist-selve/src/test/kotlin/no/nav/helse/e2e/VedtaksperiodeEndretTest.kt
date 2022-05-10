package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


internal class VedtaksperiodeEndretTest : AbstractE2ETest() {

    @Test
    fun `vedtaksperiode_endret erstatter spleis-warnings og lagrer de med verdi fra feltet tidsstempel`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot()
        vedtaksperiode(utbetalingId = UUID.randomUUID())

        val tidspunkter = finnWarningsOpprettet(VEDTAKSPERIODE_ID)
        sendVedtaksperiodeEndret(orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID)
        val tidspunkterEtterTukling = finnWarningsOpprettet(VEDTAKSPERIODE_ID)
        assertEquals(tidspunkter.size, tidspunkterEtterTukling.size)
        assertTrue(tidspunkter.values.containsAll(tidspunkterEtterTukling.values))
        assertTrue(tidspunkter.keys.none { tidspunkterEtterTukling.keys.contains(it) })
    }

    private fun finnWarningsOpprettet(vedtaksperiodeId: UUID): Map<Int, LocalDateTime> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                "SELECT * FROM warning where kilde='Spleis' AND vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?) and (inaktiv_fra is null or inaktiv_fra > now())"
            session.run(queryOf(statement, vedtaksperiodeId).map {
                it.int("id") to it.localDateTime("opprettet")
            }.asList).toMap()
        }
}
