package no.nav.helse.e2e

import AbstractE2ETest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.every
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import no.nav.helse.Meldingssender.sendArbeidsforholdløsning
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsning
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsning
import no.nav.helse.Meldingssender.sendEgenAnsattløsning
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsning
import no.nav.helse.Meldingssender.sendRisikovurderingløsning
import no.nav.helse.Meldingssender.sendVergemålløsning
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsning
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.snapshot
import no.nav.helse.mediator.graphql.HentSnapshot

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
        vedtaksperiode(vedtaksperiodeId1, snapshotV1, utbetalingId1, Periodetype.FØRSTEGANGSBEHANDLING)
        vedtaksperiode(vedtaksperiodeId2, snapshotV2, utbetalingId2, Periodetype.FORLENGELSE)

        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotFinal
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

    fun vedtaksperiode(
        vedtaksperiodeId: UUID,
        snapshot: GraphQLClientResponse<HentSnapshot.Result>,
        utbetalingId: UUID,
        periodetype: Periodetype
    ) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodetype = periodetype
        )

        if (periodetype == Periodetype.FØRSTEGANGSBEHANDLING) {
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
        }

        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId = godkjenningsmeldingId)
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
