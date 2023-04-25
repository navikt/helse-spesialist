package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtaksperiodeGenerasjonE2ETest : AbstractE2ETestV2() {

    @Test
    fun `Oppretter første generasjon når vedtaksperioden blir opprettet`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        assertGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Forventer at det eksisterer generasjon for perioden ved vedtaksperiode_endret`() {
        håndterSøknad()
        assertThrows<UninitializedPropertyAccessException> { håndterVedtaksperiodeEndret() }
        assertGenerasjoner(VEDTAKSPERIODE_ID, 0)
    }

    @Test
    fun `Oppretter ikke ny generasjon ved vedtaksperiode_endret dersom det finnes en ulåst generasjon fra før av`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeEndret()
        assertGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Låser gjeldende generasjon når perioden er godkjent og utbetalt`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        assertFerdigBehandledeGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Oppretter ny generasjon når perioden blir revurdert`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        val utbetalingId2 = UUID.randomUUID()
        håndterGodkjenningsbehov(utbetalingId = utbetalingId2, harOppdatertMetainfo = true) //revurdering
        assertGenerasjoner(VEDTAKSPERIODE_ID, 2)
        assertFerdigBehandledeGenerasjoner(VEDTAKSPERIODE_ID, 1)
    }

    @Test
    fun `Kobler til utbetaling når perioden har fått en ny utbetaling`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = UTBETALING_ID)
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 1)
    }

    @Test
    fun `Gammel utbetaling erstattes av ny utbetaling dersom perioden ikke er låst`() {
        val gammel = UUID.randomUUID()
        val ny = UUID.randomUUID()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = gammel)
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = ny)
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, gammel, 0)
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, ny, 1)
    }

    @Test
    fun `To sammenhengende perioder i revurdering, alle varsler blir godkjent når periode med oppgave blir godkjent`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val revurdertUtbetalingId = UUID.randomUUID()
        nyttVedtak(1.januar, 31.januar, vedtaksperiodeId = v1, utbetalingId = UUID.randomUUID())
        forlengVedtak(1.februar, 28.februar, skjæringstidspunkt = 1.januar, vedtaksperiodeId = v2)

        håndterVedtaksperiodeEndret(vedtaksperiodeId = v1)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = v1, utbetalingId = revurdertUtbetalingId)
        forlengelseFremTilSaksbehandleroppgave(
            1.februar,
            28.februar,
            skjæringstidspunkt = 1.januar,
            vedtaksperiodeId = v2,
            utbetalingId = revurdertUtbetalingId
        )
        håndterAktivitetsloggNyAktivitet(vedtaksperiodeId = v1, varselkoder = listOf("RV_IM_1"))
        håndterAktivitetsloggNyAktivitet(vedtaksperiodeId = v2, varselkoder = listOf("RV_IM_1"))
        håndterSaksbehandlerløsning(vedtaksperiodeId = v2)
        assertAvhukedeVarsler(v1, 1)
        assertAvhukedeVarsler(v2, 1)
    }

    @Test
    fun `fjerner knytning til utbetaling når utbetalingen blir forkastet`() {
        fremTilSaksbehandleroppgave(1.januar, 31.januar)
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 1)
        håndterUtbetalingForkastet()
        assertGenerasjonerMedUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, 0)
    }

    @Test
    fun `Flytter aktive varsler for auu`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtakFattet()
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IM_1"))

        val utbetalingId = UUID.randomUUID()
        håndterVedtaksperiodeEndret()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = utbetalingId)
        assertGenerasjoner(VEDTAKSPERIODE_ID, 2)
        assertGenerasjonHarVarsler(VEDTAKSPERIODE_ID, utbetalingId, 1)
    }

    @Test
    fun `Flytter aktive varsler for vanlige generasjoner`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IM_1"))

        val utbetalingId = UUID.randomUUID()
        håndterVedtaksperiodeEndret()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = utbetalingId)
        assertGenerasjoner(VEDTAKSPERIODE_ID, 2)
        assertGenerasjonHarVarsler(VEDTAKSPERIODE_ID, utbetalingId, 1)
    }

    private fun assertGenerasjonHarVarsler(vedtaksperiodeId: UUID, utbetalingId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
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

    private fun assertGenerasjoner(vedtaksperiodeId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ?"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall generasjoner for $vedtaksperiodeId, fant $antall" }
    }

    private fun assertFerdigBehandledeGenerasjoner(vedtaksperiodeId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND tilstand = '${Generasjon.Låst.navn()}'"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall ferdig behandlede generasjoner for $vedtaksperiodeId, fant $antall" }
    }

    private fun assertGenerasjonerMedUtbetaling(vedtaksperiodeId: UUID, utbetalingId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND utbetaling_id = ?"
            session.run(queryOf(query, vedtaksperiodeId, utbetalingId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall) { "Forventet $forventetAntall generasjoner med utbetalingId=$utbetalingId for $vedtaksperiodeId, fant $antall" }
    }
}
