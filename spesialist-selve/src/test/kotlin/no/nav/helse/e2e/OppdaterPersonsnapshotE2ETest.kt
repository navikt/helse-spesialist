package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*

internal class OppdaterPersonsnapshotE2ETest : AbstractE2ETest() {
    private val vedtaksperiodeId1: UUID = UUID.randomUUID()
    private val vedtaksperiodeId2: UUID = UUID.randomUUID()
    private val utbetalingId1: UUID = UUID.randomUUID()
    private val utbetalingId2: UUID = UUID.randomUUID()
    private val snapshotV1 = snapshot(1)
    private val snapshotV2 = snapshot(2)
    private val snapshotFinal = snapshot(3)

    @Test
    fun `Oppdater personsnapshot oppdaterer alle snapshots på personen`() {
        vedtaksperiode(vedtaksperiodeId1, snapshotV1, utbetalingId1)
        vedtaksperiode(vedtaksperiodeId2, snapshotV2, utbetalingId2)

        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns snapshotFinal
        sendOppdaterPersonsnapshot()

        assertSnapshot(snapshotFinal, vedtaksperiodeId1)
        assertSnapshot(snapshotFinal, vedtaksperiodeId2)
    }

    private fun sendOppdaterPersonsnapshot() {
        @Language("JSON")
        val json = """
{
    "@event_name": "oppdater_personsnapshot",
    "@id": "${UUID.randomUUID()}",
    "fødselsnummer": "$FØDSELSNUMMER"
}"""

        testRapid.sendTestMessage(json)
    }

    fun vedtaksperiode(vedtaksperiodeId: UUID, snapshot: String, utbetalingId: UUID) {

        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = utbetalingId
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId
        )
    }
}
