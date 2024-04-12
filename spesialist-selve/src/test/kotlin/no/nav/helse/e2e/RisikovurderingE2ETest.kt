package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Risikofunn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class RisikovurderingE2ETest : AbstractE2ETest() {

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
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(risikofunn = funnSomKreverRiskTilgang)
        assertOppgaveHarEgenskap("RISK_QA", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(risikofunn = funnSomAlleKanBehandle)
        assertOppgaveHarEgenskap("SØKNAD", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `sender med kunRefusjon`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        assertInnholdIBehov(behov = "Risikovurdering") { jsonNode ->
            assertTrue(jsonNode["Risikovurdering"]["kunRefusjon"].asBoolean())
        }
    }

    private fun assertOppgaveHarEgenskap(forventet: String, vedtaksperiodeId: UUID) {
        val egenskaper = sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query =
                "SELECT egenskaper FROM oppgave JOIN vedtak on vedtak.id = vedtak_ref WHERE vedtaksperiode_id = :vedtaksperiodeId"
            it.run(
                queryOf(
                    query,
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId)
                ).map { row -> row.array<String>("egenskaper") }.asSingle
            )
        }
        assertTrue(egenskaper?.contains(forventet) ?: false)
    }

}
