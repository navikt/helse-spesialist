package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import no.nav.helse.graphQLSnapshot
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class VersjoneringAvSnapshotTest: AbstractE2ETest() {

    @Test
    fun `utdatert snapshot`() {
        val utbetalingId = UUID.randomUUID()
        val gammelSnapshot = snapshot(-1)
        val nyttSnapshot = snapshot(2)
        vedtaksperiode(snapshot = gammelSnapshot, utbetalingId = utbetalingId)
        every { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns nyttSnapshot
        every { speilSnapshotGraphQLClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, "1234")

        personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false)
        verify(exactly = 1) { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) }

        clearMocks(speilSnapshotRestClient)
        personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false)
        verify(exactly = 0) { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `utdatert snapshot fra aktørId`() {
        val utbetalingId = UUID.randomUUID()
        val gammelSnapshot = snapshot(-1)
        val nyttSnapshot = snapshot(2)
        vedtaksperiode(snapshot = gammelSnapshot, utbetalingId = utbetalingId)
        every { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns nyttSnapshot
        every { speilSnapshotGraphQLClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, "1234")

        personMediator.byggSpeilSnapshotForAktørId(AKTØR, false)
        verify(exactly = 1) { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `utdatert snapshot fra vedtaksperiodeid`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeid = UUID.randomUUID()
        val gammelSnapshot = snapshot(-1)
        val nyttSnapshot = snapshot(2)
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeid, snapshot = gammelSnapshot, utbetalingId = utbetalingId)
        every { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns nyttSnapshot
        every { speilSnapshotGraphQLClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, "1234")

        personMediator.byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeid, false)
        verify(exactly = 1) { speilSnapshotRestClient.hentSpeilSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `fnr finnes ikke`() {
        val actual = personMediator.byggSpeilSnapshotForFnr("77889900", false).snapshot
        assertNull(actual)
        verify(exactly = 0) { speilSnapshotRestClient.hentSpeilSnapshot("77889900") }
    }
}
