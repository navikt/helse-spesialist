package no.nav.helse.e2e

import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.modell.oppgave.Egenskap
import org.junit.jupiter.api.Test

internal class ManglerInntektsmeldingE2ETest : AbstractE2ETest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("RV_IV_10"))

        assertHarOppgaveegenskap(inspektør.oppgaveId(), Egenskap.MANGLER_IM)
    }
}
