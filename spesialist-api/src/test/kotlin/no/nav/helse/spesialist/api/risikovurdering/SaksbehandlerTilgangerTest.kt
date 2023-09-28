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
    private val skjermedePersonerSaksbehandlergruppe = UUID.randomUUID()
    private val saksbehandlereMedTilgangTilStikkprøve = UUID.randomUUID()
    private val saksbehandlereMedTilgangTilSpesialsaker = UUID.randomUUID()

    @Test
    fun `uten gruppetilhørighet har man ingen tilganger`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
            stikkprøveSaksbehandlergruppe = saksbehandlereMedTilgangTilStikkprøve,
            spesialsakSaksbehandlergruppe = saksbehandlereMedTilgangTilSpesialsaker
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med kode7-gruppetilgang skal man kunne løse kode7-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
            stikkprøveSaksbehandlergruppe = saksbehandlereMedTilgangTilStikkprøve,
            spesialsakSaksbehandlergruppe = saksbehandlereMedTilgangTilSpesialsaker
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med risk-gruppetilgang skal man kunne løse risk-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(riskSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
            stikkprøveSaksbehandlergruppe = saksbehandlereMedTilgangTilStikkprøve,
            spesialsakSaksbehandlergruppe = saksbehandlereMedTilgangTilSpesialsaker
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med beslutter-gruppetilgang skal man kunne løse beslutter-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(beslutterSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
            stikkprøveSaksbehandlergruppe = saksbehandlereMedTilgangTilStikkprøve,
            spesialsakSaksbehandlergruppe = saksbehandlereMedTilgangTilSpesialsaker
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med skjermede personer-gruppetilgang skal man kunne løse skjermede personer-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(skjermedePersonerSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
            stikkprøveSaksbehandlergruppe = saksbehandlereMedTilgangTilStikkprøve,
            spesialsakSaksbehandlergruppe = saksbehandlereMedTilgangTilSpesialsaker
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med alle gruppetilganger skal man kunne løse alle oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(
                riskSaksbehandlergruppe,
                kode7Saksbehandlergruppe,
                beslutterSaksbehandlergruppe,
                skjermedePersonerSaksbehandlergruppe
            ),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
            stikkprøveSaksbehandlergruppe = saksbehandlereMedTilgangTilStikkprøve,
            spesialsakSaksbehandlergruppe = saksbehandlereMedTilgangTilSpesialsaker
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7())
        assertTrue(saksbehandlerTilganger.harTilgangTilRiskOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }
}
