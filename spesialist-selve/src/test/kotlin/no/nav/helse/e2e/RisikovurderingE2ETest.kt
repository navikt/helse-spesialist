package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.meldinger.Risikofunn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class RisikovurderingE2ETest : AbstractE2ETestV2() {

    private val funnSomKreverRiskTilgang = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivelse = "ny sjekk ikke ok",
        kreverSupersaksbehandler = true
    ))

    private val funnSomAlleKanBehandle = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivelse = "8-4 ikke ok",
        kreverSupersaksbehandler = false
    ))

    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        fremTilSaksbehandleroppgave(risikofunn = funnSomKreverRiskTilgang)
        assertOppgaveType("RISK_QA", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        fremTilSaksbehandleroppgave(risikofunn = funnSomAlleKanBehandle)
        assertOppgaveType("SØKNAD", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `sender med kunRefusjon`() {
        fremTilSaksbehandleroppgave()
        assertInnholdIBehov(behov = "Risikovurdering") { jsonNode ->
            assertTrue(jsonNode["Risikovurdering"]["kunRefusjon"].asBoolean())
        }
    }

    private fun assertOppgaveType(forventet: String, vedtaksperiodeId: UUID) =
        assertEquals(forventet, sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT type FROM oppgave JOIN vedtak on vedtak.id = vedtak_ref WHERE vedtaksperiode_id = :vedtaksperiodeId",
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId)
                ).map { row -> row.string("type") }.asSingle
            )
        })
}
