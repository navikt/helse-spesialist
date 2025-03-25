package no.nav.helse.spesialist.e2etests

import org.junit.jupiter.api.Test

class VedtakFattetE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `vedtak fattet medfører låsing av vedtaksperiode-generasjon`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        simulerFremTilOgMedGodkjenningsbehov()

        callGraphQL(
            operationName = "Tildeling",
            variables = mapOf(
                "oppgavereferanse" to finnOppgaveId().toString(),
            )
        )

        // When:
        callGraphQL(
            operationName = "SettVarselStatus",
            variables = mapOf(
                "generasjonIdString" to finnGenerasjonId(),
                "varselkode" to "SB_RV_1",
                "ident" to saksbehandler.ident,
                "definisjonIdString" to "77970f04-c4c5-4b9f-8795-bb5e4749344c", // id fra api varseldefinisjon
            )
        )
        callGraphQL(
            operationName = "FattVedtak",
            variables = mapOf(
                "oppgavereferanse" to finnOppgaveId().toString(),
                "begrunnelse" to "Fattet vedtak",
            )
        )

        // Then:
        assertBehandlingTilstand("VedtakFattet")
    }
}
