package no.nav.helse.spesialist.e2etests

import no.nav.helse.modell.oppgave.Egenskap
import org.junit.jupiter.api.Test

class ManglerInntektsmeldingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        // Given:
        lagreVarseldefinisjon("RV_IV_10")

        // When:
        val spleisBehandlingId = simulerFremTilOgMedNyUtbetaling()
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("RV_IV_10"))
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(spleisBehandlingId)

        // Then:
        assertHarOppgaveegenskap(Egenskap.MANGLER_IM)
    }
}
