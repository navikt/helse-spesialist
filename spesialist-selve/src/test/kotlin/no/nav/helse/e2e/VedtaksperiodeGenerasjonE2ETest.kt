package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtaksperiodeGenerasjonE2ETest : AbstractE2ETest() {
    @Test
    fun `Oppretter første generasjon når vedtaksperioden blir opprettet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        assertGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Oppretter ikke ny generasjon ved vedtaksperiode_endret dersom det finnes en ubehandlet generasjon fra før av`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeEndret()
        assertGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Låser gjeldende generasjon når perioden er godkjent og utbetalt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        assertFerdigBehandledeGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Oppretter ny generasjon når perioden blir revurdert`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        spleisOppretterNyBehandling()
        assertGenerasjoner(VEDTAKSPERIODE_ID, 2)
        assertFerdigBehandledeGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Kobler til utbetaling når perioden har fått en ny utbetaling`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = UTBETALING_ID)
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 1)
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
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, gammel, 0)
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, ny, 1)
    }

    @Test
    fun `fjerner knytning til utbetaling når utbetalingen blir forkastet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 1)
        håndterUtbetalingForkastet()
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 0)
    }

    @Test
    fun `Flytter aktive varsler for auu`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterAvsluttetUtenVedtak()
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IM_1"))

        val utbetalingId = UUID.randomUUID()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = utbetalingId)
        assertGenerasjoner(VEDTAKSPERIODE_ID, 2)
        assertGenerasjonHarVarsler(VEDTAKSPERIODE_ID, utbetalingId, 1)
    }

    @Test
    fun `Flytter aktive varsler for vanlige generasjoner`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IM_1"))

        val utbetalingId = UUID.randomUUID()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = utbetalingId)
        assertGenerasjoner(VEDTAKSPERIODE_ID, 2)
        assertGenerasjonHarVarsler(VEDTAKSPERIODE_ID, utbetalingId, 1)
    }

    private fun assertGenerasjonHarVarsler(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query =
                    """
                    SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg 
                    INNER JOIN selve_varsel sv on svg.id = sv.generasjon_ref 
                    WHERE svg.vedtaksperiode_id = ? AND utbetaling_id = ?
                    """
                session.run(queryOf(query, vedtaksperiodeId, utbetalingId).map { it.int(1) }.asSingle)
            }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall varsler for $vedtaksperiodeId, $utbetalingId, fant $antall" }
    }

    private fun assertGenerasjoner(
        vedtaksperiodeId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ?"
                session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
            }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall generasjoner for $vedtaksperiodeId, fant $antall" }
    }

    private fun assertFerdigBehandledeGenerasjoner(
        vedtaksperiodeId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND tilstand = '${Generasjon.VedtakFattet.navn()}'"
                session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
            }
        assertEquals(
            forventetAntall,
            antall,
        ) { "Forventet $forventetAntall ferdig behandlede generasjoner for $vedtaksperiodeId, fant $antall" }
    }

    private fun assertGenerasjonerMedUtbetaling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND utbetaling_id = ?"
                session.run(queryOf(query, vedtaksperiodeId, utbetalingId).map { it.int(1) }.asSingle)
            }
        assertEquals(forventetAntall, antall) {
            "Forventet $forventetAntall generasjoner med utbetalingId=$utbetalingId for $vedtaksperiodeId, fant $antall"
        }
    }
}
