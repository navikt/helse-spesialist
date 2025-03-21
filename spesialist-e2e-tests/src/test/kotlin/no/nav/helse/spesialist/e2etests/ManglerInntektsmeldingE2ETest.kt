package no.nav.helse.spesialist.e2etests

import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.Test

class ManglerInntektsmeldingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        val testPerson = TestPerson()
        vedtaksløsningenMottarNySøknad(testPerson)
        spleisOppretterNyBehandling(testPerson = testPerson)
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("RV_IV_10"))

        assertHarOppgaveegenskap(Egenskap.MANGLER_IM)
    }
}
