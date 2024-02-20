package no.nav.helse.mediator

import DatabaseIntegrationTest
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
internal class VersjoneringAvSnapshotTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun resetGlobalSnapshotVersjon() {
        query("update global_snapshot_versjon set versjon = 1 where id = 1").execute()
    }

    private val snapshotApiDao = SnapshotApiDao(dataSource)
    private val snapshotClient = mockk<SnapshotClient>()
    private val snapshotMediator = SnapshotMediator(
        snapshotApiDao,
        snapshotClient = snapshotClient
    )

    @Test
    fun `utdatert snapshot`() {
        every { snapshotClient.hentSnapshot(FNR) } returns snapshot()

        nyPerson()
        opprettSnapshot(person = snapshot(fødselsnummer = FNR, aktørId = AKTØR, versjon = 0).data?.person!!)

        snapshotMediator.hentSnapshot(FNR)

        verify(exactly = 1) { snapshotClient.hentSnapshot(FNR) }

        snapshotMediator.hentSnapshot(FNR)

        confirmVerified(snapshotClient)
    }

}
