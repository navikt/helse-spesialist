package no.nav.helse.e2e

import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RisikovurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        val riskfunn = listOf(Risikofunn(kategori = emptyList(), beskrivelse = "et faresignal"))

        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(risikofunn = riskfunn)
        assertHarOppgaveegenskap(inspektør.oppgaveId(), RISK_QA)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("TESTKODE_42"))
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(kanGodkjennesAutomatisk = true)
        assertHarOppgaveegenskap(inspektør.oppgaveId(), SØKNAD)
        assertHarIkkeOppgaveegenskap(inspektør.oppgaveId(), RISK_QA)
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

    @Test
    fun `sender med inntekt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        assertInnholdIBehov(behov = "Risikovurdering") { jsonNode ->
            assertEquals("Arbeidsgiver", jsonNode["Risikovurdering"]["inntekt"]["inntektskilde"].asText())
            assertEquals(123456.7, jsonNode["Risikovurdering"]["inntekt"]["omregnetÅrsinntekt"].asDouble())
        }
    }
}
