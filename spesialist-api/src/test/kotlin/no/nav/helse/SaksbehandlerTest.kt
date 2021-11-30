package no.nav.helse

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class SaksbehandlerTest {
    private val kode7Saksbehandlergruppe = UUID.randomUUID()
    private val riskSaksbehandlergruppe = UUID.randomUUID()

    @Test
    fun `uten gruppetilhørighet har man ingen tilganger`() {
        val saksbehandler = Saksbehandler(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertFalse(saksbehandler.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandler.harTilgangTilRiskOppgaver())
    }

    @Test
    fun `med kode7-gruppetilgang skal man kunne løse kode7-oppgaver`() {
        val saksbehandler = Saksbehandler(
            gruppetilganger = listOf(kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertTrue(saksbehandler.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandler.harTilgangTilRiskOppgaver())
    }

    @Test
    fun `med risk-gruppetilgang skal man kunne løse risk-oppgaver`() {
        val saksbehandler = Saksbehandler(
            gruppetilganger = listOf(riskSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertFalse(saksbehandler.harTilgangTilKode7Oppgaver())
        assertTrue(saksbehandler.harTilgangTilRiskOppgaver())
    }

    @Test
    fun `med alle gruppetilganger skal man kunne løse alle oppgaver`() {
        val saksbehandler = Saksbehandler(
            gruppetilganger = listOf(riskSaksbehandlergruppe, kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe
        )
        assertTrue(saksbehandler.harTilgangTilKode7Oppgaver())
        assertTrue(saksbehandler.harTilgangTilRiskOppgaver())
    }
}
