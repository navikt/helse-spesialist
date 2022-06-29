package no.nav.helse.spesialist.api.risikovurdering

import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SaksbehandlerTilgangerTest {
    private val kode7Saksbehandlergruppe = UUID.randomUUID()
    private val riskSaksbehandlergruppe = UUID.randomUUID()
    private val beslutterSaksbehandlergruppe = UUID.randomUUID()

    @Test
    fun `uten gruppetilhørighet har man ingen tilganger`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
    }

    @Test
    fun `med kode7-gruppetilgang skal man kunne løse kode7-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
    }

    @Test
    fun `med risk-gruppetilgang skal man kunne løse risk-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(riskSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
    }

    @Test
    fun `med beslutter-gruppetilgang skal man kunne løse beslutter-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(beslutterSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
    }

    @Test
    fun `med alle gruppetilganger skal man kunne løse alle oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(riskSaksbehandlergruppe, kode7Saksbehandlergruppe, beslutterSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7Oppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
    }
}
