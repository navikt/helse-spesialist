package no.nav.helse

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class SaksbehandlerTilgangerTest {
    private val kode7Saksbehandlergruppe = UUID.randomUUID()
    private val riskSaksbehandlergruppe = UUID.randomUUID()

    @Test
    fun `uten gruppetilhørighet har man ingen tilganger`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.kanSeAlleOppgaver())
    }

    @Test
    fun `med kode7-gruppetilgang skal man kunne løse kode7-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.kanSeAlleOppgaver())
    }

    @Test
    fun `med risk-gruppetilgang skal man kunne løse risk-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(riskSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.kanSeAlleOppgaver())
    }

    @Test
    fun `med alle gruppetilganger skal man kunne løse alle oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(riskSaksbehandlergruppe, kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.kanSeAlleOppgaver())
    }

    @Test
    fun `kan se alle oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            NAVident = "N115007"
        )
        assertTrue(saksbehandlerTilganger.kanSeAlleOppgaver())
    }
}
