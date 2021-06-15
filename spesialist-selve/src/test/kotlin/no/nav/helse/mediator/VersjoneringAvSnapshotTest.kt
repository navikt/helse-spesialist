package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
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
        every { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns nyttSnapshot

        vedtaksperiodeMediator.byggSpeilSnapshotForFnr(UNG_PERSON_FNR_2018)
        verify(exactly = 1) { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }

        clearMocks(speilSnapshotRestClient)
        vedtaksperiodeMediator.byggSpeilSnapshotForFnr(UNG_PERSON_FNR_2018)
        verify(exactly = 0) { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `utdatert snapshot fra aktørId`() {
        val utbetalingId = UUID.randomUUID()
        val gammelSnapshot = snapshot(-1)
        val nyttSnapshot = snapshot(2)
        vedtaksperiode(snapshot = gammelSnapshot, utbetalingId = utbetalingId)
        every { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns nyttSnapshot

        vedtaksperiodeMediator.byggSpeilSnapshotForAktørId(AKTØR)
        verify(exactly = 1) { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `utdatert snapshot fra vedtaksperiodeid`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeid = UUID.randomUUID()
        val gammelSnapshot = snapshot(-1)
        val nyttSnapshot = snapshot(2)
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeid, snapshot = gammelSnapshot, utbetalingId = utbetalingId)
        every { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns nyttSnapshot

        vedtaksperiodeMediator.byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeid)
        verify(exactly = 1) { speilSnapshotRestClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `fnr finnes ikke`() {
        val actual = vedtaksperiodeMediator.byggSpeilSnapshotForFnr("77889900")
        assertNull(actual)
        verify(exactly = 0) { speilSnapshotRestClient.hentSpeilSpapshot("77889900") }
    }
}
