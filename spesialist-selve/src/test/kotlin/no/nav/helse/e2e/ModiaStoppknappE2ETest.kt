package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test

internal class ModiaStoppknappE2ETest : AbstractE2ETest() {
    @Test
    fun `Går til manuell når knappen er trykket på`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(stoppKnappTrykket = true, kanGodkjennesAutomatisk = true)
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `Automatisk godkjenning når knappen ikke er trykket på`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()

        // Denne settes til true for å sikre at det er sparkel-stoppknapp som er utslagsgivende, ikke risk-modulen
        val statusFraRisk = true
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            stoppKnappTrykket = false,
            kanGodkjennesAutomatisk = statusFraRisk,
        )
        assertSaksbehandleroppgaveBleIkkeOpprettet()
    }
}
