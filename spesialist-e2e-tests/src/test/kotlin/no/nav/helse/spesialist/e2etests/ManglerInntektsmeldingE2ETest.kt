package no.nav.helse.spesialist.e2etests

import no.nav.helse.modell.oppgave.Egenskap
import org.junit.jupiter.api.Test
import java.util.UUID

class ManglerInntektsmeldingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        // Given:
        lagreVarseldefinisjon("RV_IV_10")

        // When:
        simulerPublisertSendtSøknadNavMelding()
        val spleisBehandlingId = UUID.randomUUID()
        simulerPublisertBehandlingOpprettetMelding(spleisBehandlingId = spleisBehandlingId)
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("RV_IV_10"))
        simulerPublisertVedtaksperiodeNyUtbetalingMelding()
        simulerPublisertUtbetalingEndretMelding()
        simulerPublisertVedtaksperiodeEndretMelding()
        simulerPublisertGodkjenningsbehovMelding(spleisBehandlingId = spleisBehandlingId)

        sendEgenAnsattløsning()
        sendVergemålOgFullmaktløsning()
        sendÅpneGosysOppgaverløsning()
        sendRisikovurderingløsning()
        sendInntektløsning()

        // Then:
        assertHarOppgaveegenskap(Egenskap.MANGLER_IM)
    }
}
