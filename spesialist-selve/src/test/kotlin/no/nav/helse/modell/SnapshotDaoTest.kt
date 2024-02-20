package no.nav.helse.modell

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
internal class SnapshotDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun resetGlobalSnapshotVersjon() {
        query("update global_snapshot_versjon set versjon = 1 where id = 1").update()
    }

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
        val snapshot = snapshot(versjon = 1).data!!.person!!
        val nyttSnapshot = snapshot(versjon = 2).data!!.person!!
        opprettPerson()
        snapshotDao.lagre(FNR, snapshot)
        snapshotDao.lagre(FNR, nyttSnapshot)

        val alleSnapshots = finnAlleSnapshot()
        assertEquals(1, alleSnapshots.size)
        assertEquals(nyttSnapshot, alleSnapshots.first().first)
        assertEquals(2, alleSnapshots.first().second)
        assertEquals(2, globaltVersjonsnummer())
    }

    private fun finnAlleSnapshot(fødselsnummer: String = FNR) = query(
        """
            SELECT data, versjon FROM snapshot s
            JOIN person p ON s.person_ref = p.id
            WHERE p.fodselsnummer = :fnr
        """.trimIndent(), "fnr" to fødselsnummer.toLong()
    ).list { objectMapper.readValue<GraphQLPerson>(it.string("data")) to it.int("versjon") }


    private fun globaltVersjonsnummer() =
        query("SELECT versjon FROM global_snapshot_versjon").single { it.int("versjon") }
}
