package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import no.nav.helse.oppgave.Oppgavestatus
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class OppdaterPersonsnapshotMedWarningsE2ETest : AbstractE2ETest() {
    private val snapshotFinal = snapshot(3)

    @Test
    fun `Oppdater personsnapshot oppdaterer alle snapshots på personen`() {
        settOppBruker()
        assertOppgaver(1)
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler)

        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotFinal
        sendOppdaterPersonsnapshotMedWarnings()

        assertSnapshot(snapshotFinal, VEDTAKSPERIODE_ID)
    }

    private fun sendOppdaterPersonsnapshotMedWarnings() {
        @Language("JSON")
        val json = """
{
    "@event_name": "oppdater_personsnapshot_med_warnings",
    "@id": "${UUID.randomUUID()}",
    "fødselsnummer": "$FØDSELSNUMMER",
    "vedtaksperiodeId": "$VEDTAKSPERIODE_ID"
}"""
        testRapid.sendTestMessage(json)
    }
}
