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
        val snapshot = snapshot()
        opprettPerson()
        speilSnapshotDao.lagre(FNR, snapshot)
        assertEquals(1, finnAlleSnapshot().size)
        assertEquals(snapshot, finnAlleSnapshot().first().first)
        assertEquals(LocalDate.now(), finnAlleSnapshot().first().second)
    }

    @Test
    fun `oppdaterer globalt og pr snapshot dersom utdatert`() {
        val snapshot = snapshot(1)
        val nyttSnapshot = snapshot(2)
        opprettPerson()
        speilSnapshotDao.lagre(FNR, snapshot)
        speilSnapshotDao.lagre(FNR, nyttSnapshot)

        val alleSnapshots = finnAlleSnapshot()
        assertEquals(1, alleSnapshots.size)
        assertEquals(nyttSnapshot, alleSnapshots.first().first)
        assertEquals(LocalDate.now(), alleSnapshots.first().second)
        assertEquals(2, alleSnapshots.first().third)
        assertEquals(2, globaltVersjonsnummer())
    }

    private fun finnAlleSnapshot() = sessionOf(dataSource).use  {
        it.run(queryOf("SELECT data, sist_endret, versjon FROM speil_snapshot").map {
            Triple(it.string("data"), it.localDate("sist_endret"), it.int("versjon"))
        }.asList)
    }

    private fun globaltVersjonsnummer() = sessionOf(dataSource).use {
        it.run(queryOf("SELECT versjon FROM global_snapshot_versjon").map { it.int("versjon") }.asSingle)
    }
}
