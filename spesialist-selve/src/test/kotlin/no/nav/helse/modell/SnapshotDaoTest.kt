package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SnapshotDaoTest : DatabaseIntegrationTest() {


    @Test
    fun `lagre snapshot`() {
        val personBlob = "{}"
        snapshotDao.insertSpeilSnapshot(personBlob)
        assertEquals(1, snapshot().size)
        assertEquals(personBlob, snapshot().first().first)
        assertEquals(LocalDate.now(), snapshot().first().second)
    }

    private fun snapshot() = sessionOf(dataSource).use  {
        it.run(queryOf("SELECT data, sist_endret FROM speil_snapshot").map {
            it.string("data") to it.localDate("sist_endret")
        }.asList)
    }
}
