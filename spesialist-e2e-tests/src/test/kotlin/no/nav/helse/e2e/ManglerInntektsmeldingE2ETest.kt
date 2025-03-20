package no.nav.helse.e2e

import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.modell.oppgave.Egenskap
import org.junit.jupiter.api.Test

class ManglerInntektsmeldingE2ETest : AbstractE2ETest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        vedtaksløsningenMottarNySøknad(fødselsnummer = "31111111111")
        spleisOppretterNyBehandling(fødselsnummer = "31111111111")
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(fødselsnummer = "31111111111"), regelverksvarsler = listOf("RV_IV_10"))

        assertHarOppgaveegenskap(inspektør.oppgaveId(), Egenskap.MANGLER_IM)
    }
}
