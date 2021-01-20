package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.modell.Oppgavestatus
import no.nav.helse.snapshotUtenWarnings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

internal class AvbrytSaksbehandlingE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private val SNAPSHOTV1 = snapshotUtenWarnings(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `avbryter saksbehandling før oppgave er opprettet til saksbehandling`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )

        sendAvbrytSaksbehandling(UNG_PERSON_FNR_2018, VEDTAKSPERIODE_ID)

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "AVBRUTT"
        )
        assertOppgaver(0)
    }

    @Test
    fun `avbryter saksbehandling etter oppgave er opprettet til saksbehandling`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = vedtaksperiodeTilGodkjenning()

        sendAvbrytSaksbehandling(UNG_PERSON_FNR_2018, VEDTAKSPERIODE_ID)

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Invalidert)
    }

    @Test
    fun `oppretter oppgave hos saksbehandler andre runde`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        var godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )

        sendAvbrytSaksbehandling(UNG_PERSON_FNR_2018, VEDTAKSPERIODE_ID)

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "AVBRUTT"
        )
        assertOppgaver(0)

        godkjenningsmeldingId = vedtaksperiodeTilGodkjenning()

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )

        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `avbryt ikke-eksisterende vedtaksperiode`() =
        assertDoesNotThrow { sendAvbrytSaksbehandling(UNG_PERSON_FNR_2018, VEDTAKSPERIODE_ID) }

    private fun vedtaksperiodeTilGodkjenning(): UUID {
        val godkjenningsmeldingId1 = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId1, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erEgenAnsatt = false
        )

        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erDigital = true
        )

        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1, 1
        )
        return godkjenningsmeldingId1
    }
}
