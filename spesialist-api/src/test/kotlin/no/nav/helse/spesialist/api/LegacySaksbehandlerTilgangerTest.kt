package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.domain.tilgangskontroll.SaksbehandlerTilganger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class LegacySaksbehandlerTilgangerTest {
    private val kode7Saksbehandlergruppe = UUID.randomUUID()
    private val beslutterSaksbehandlergruppe = UUID.randomUUID()
    private val skjermedePersonerSaksbehandlergruppe = UUID.randomUUID()

    @Test
    fun `uten gruppetilhørighet har man ingen tilganger`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med kode7-gruppetilgang skal man kunne løse kode7-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(kode7Saksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med beslutter-gruppetilgang skal man kunne løse beslutter-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(beslutterSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertTrue(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertFalse(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med skjermede personer-gruppetilgang skal man kunne løse skjermede personer-oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(skjermedePersonerSaksbehandlergruppe),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
        )
        assertFalse(saksbehandlerTilganger.harTilgangTilKode7())
        assertFalse(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }

    @Test
    fun `med alle gruppetilganger skal man kunne løse alle oppgaver`() {
        val saksbehandlerTilganger = SaksbehandlerTilganger(
            gruppetilganger = listOf(
                kode7Saksbehandlergruppe,
                beslutterSaksbehandlergruppe,
                skjermedePersonerSaksbehandlergruppe
            ),
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
        )
        assertTrue(saksbehandlerTilganger.harTilgangTilKode7())
        assertTrue(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver())
        assertTrue(saksbehandlerTilganger.harTilgangTilSkjermedePersoner())
    }
}
