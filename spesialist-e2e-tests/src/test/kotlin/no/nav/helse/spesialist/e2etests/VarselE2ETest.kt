package no.nav.helse.spesialist.e2etests

import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.INAKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_RV_1
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class VarselE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ingen varsel`() {
        // Given:

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(emptySet(), hentVarselkoder())
    }

    @Test
    fun `varsel om faresignaler ved risikovurdering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.funn = listOf(Risikofunn(listOf("EN_KATEGORI"), "EN_BESKRIVELSE"))

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(setOf(Varsel(SB_RV_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lager ikke duplikatvarsel ved flere svar på samme behov for åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        behovLøserStub.besvarIgjen("ÅpneOppgaver")

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `fjern varsel om gosys-oppgave dersom det ikke finnes gosys-oppgave lenger`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        åpneOppgaverBehovLøser.antall = 0
        simulerPublisertGosysOppgaveEndretMelding()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, INAKTIV.name)), hentVarselkoder())
    }

    private fun simulerFremTilOgMedGodkjenningsbehov() {
        simulerPublisertSendtSøknadNavMelding()
        val spleisBehandlingId = UUID.randomUUID()
        simulerPublisertBehandlingOpprettetMelding(spleisBehandlingId = spleisBehandlingId)
        simulerPublisertVedtaksperiodeNyUtbetalingMelding()
        simulerPublisertUtbetalingEndretMelding()
        simulerPublisertVedtaksperiodeEndretMelding()
        simulerPublisertGodkjenningsbehovMelding(spleisBehandlingId = spleisBehandlingId)
    }
}
