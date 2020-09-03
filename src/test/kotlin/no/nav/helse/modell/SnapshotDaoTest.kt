package no.nav.helse.modell

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SnapshotDaoTest : AbstractEndToEndTest() {

    private lateinit var dao: SnapshotDao

    @BeforeEach
    fun setup() {
        dao = SnapshotDao(dataSource)
    }

    @Test
    fun `lagre snapshot`() {
        val personBlob = "{}"
        dao.insertSpeilSnapshot(personBlob)
        assertEquals(1, snapshot().size)
        assertEquals(personBlob, snapshot().first().first)
        assertEquals(LocalDate.now(), snapshot().first().second)
    }

    private fun snapshot() = using(sessionOf(dataSource)) {
        it.run(queryOf("SELECT data, sist_endret FROM speil_snapshot").map {
            it.string("data") to it.localDate("sist_endret")
        }.asList)
    }
}
