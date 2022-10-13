package no.nav.helse.modell

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SnapshotDaoTest : DatabaseIntegrationTest() {


    @Test
    fun `lagre snapshot`() {
        val snapshot = snapshot().data!!.person!!
        opprettPerson()
        snapshotDao.lagre(FNR, snapshot)
        assertEquals(1, finnAlleSnapshot().size)
        assertEquals(snapshot, finnAlleSnapshot().first().first)
    }

    @Test
    fun `oppdaterer globalt og pr snapshot dersom utdatert`() {
        val snapshot = snapshot(1).data!!.person!!
        val nyttSnapshot = snapshot(2).data!!.person!!
        opprettPerson()
        snapshotDao.lagre(FNR, snapshot)
        snapshotDao.lagre(FNR, nyttSnapshot)

        val alleSnapshots = finnAlleSnapshot()
        assertEquals(1, alleSnapshots.size)
        assertEquals(nyttSnapshot, alleSnapshots.first().first)
        assertEquals(2, alleSnapshots.first().second)
        assertEquals(2, globaltVersjonsnummer())
    }

    private fun finnAlleSnapshot() = sessionOf(dataSource).use  { session ->
        session.run(queryOf("SELECT data, versjon FROM snapshot").map {
            Pair(objectMapper.readValue<GraphQLPerson>(it.string("data")), it.int("versjon"))
        }.asList)
    }

    private fun globaltVersjonsnummer() = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT versjon FROM global_snapshot_versjon").map { it.int("versjon") }.asSingle)
    }
}
