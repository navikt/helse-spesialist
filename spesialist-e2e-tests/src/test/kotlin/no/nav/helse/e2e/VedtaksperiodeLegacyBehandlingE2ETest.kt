package no.nav.helse.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtaksperiodeLegacyBehandlingE2ETest : AbstractE2ETest() {
    @Test
    fun `Oppretter første behandling når vedtaksperioden blir opprettet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        assertBehandlinger(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Oppretter ikke ny behandling ved vedtaksperiode_endret dersom det finnes en ubehandlet behandling fra før av`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeEndret()
        assertBehandlinger(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Kobler til utbetaling når perioden har fått en ny utbetaling`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = UTBETALING_ID)
        assertBehandlingerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 1)
    }

    @Test
    fun `Gammel utbetaling erstattes av ny utbetaling dersom perioden ikke er ferdig behandlet`() {
        val gammel = UUID.randomUUID()
        val ny = UUID.randomUUID()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = gammel)
        håndterUtbetalingForkastet()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = ny)
        assertBehandlingerMedUtbetaling(VEDTAKSPERIODE_ID, gammel, 0)
        assertBehandlingerMedUtbetaling(VEDTAKSPERIODE_ID, ny, 1)
    }

    @Test
    fun `fjerner knytning til utbetaling når utbetalingen blir forkastet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        assertBehandlingerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 1)
        håndterUtbetalingForkastet()
        assertBehandlingerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 0)
    }

    @Test
    fun `Flytter aktive varsler for auu`() {
        vedtaksløsningenMottarNySøknad()
        val spleisBehandlingId = UUID.randomUUID()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        håndterAvsluttetUtenVedtak(spleisBehandlingId = spleisBehandlingId)
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IM_1"))

        val utbetalingId = UUID.randomUUID()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = utbetalingId)
        assertBehandlinger(VEDTAKSPERIODE_ID, 2)
        assertBehandlingHarVarsler(VEDTAKSPERIODE_ID, utbetalingId, 1)
    }

    private fun assertBehandlingHarVarsler(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                """
                SELECT COUNT(*) FROM behandling b 
                INNER JOIN varsel sv on b.id = sv.behandling_ref 
                WHERE b.vedtaksperiode_id = :vedtaksperiodeId AND utbetaling_id = :utbetalingId AND sv.status = 'AKTIV'
                """,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "utbetalingId" to utbetalingId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall varsler for $vedtaksperiodeId, $utbetalingId, fant $antall" }
    }

    private fun assertBehandlinger(
        vedtaksperiodeId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                "SELECT COUNT(*) FROM behandling WHERE vedtaksperiode_id = :vedtaksperiodeId",
                "vedtaksperiodeId" to vedtaksperiodeId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall behandlinger for $vedtaksperiodeId, fant $antall" }
    }

    private fun assertBehandlingerMedUtbetaling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                "SELECT COUNT(*) FROM behandling WHERE vedtaksperiode_id = :vedtaksperiodeId AND utbetaling_id = :utbetalingId",
                "vedtaksperiodeId" to vedtaksperiodeId,
                "utbetalingId" to utbetalingId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall) {
            "Forventet $forventetAntall behandlinger med utbetalingId=$utbetalingId for $vedtaksperiodeId, fant $antall"
        }
    }
}
