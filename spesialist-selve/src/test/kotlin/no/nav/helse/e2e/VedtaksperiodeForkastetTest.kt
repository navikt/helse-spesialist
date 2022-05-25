package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals

internal class VedtaksperiodeForkastetTest : AbstractE2ETest() {
    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `VedtaksperiodeForkastet oppdaterer ikke oppgave-tabellen dersom status er inaktiv`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot()
        vedtaksperiodeTilGodkjenning()

        sendSaksbehandlerløsning(OPPGAVEID, "", "", UUID.randomUUID(), true)
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
        val tidspunktVedFerdigstilling = oppgaveOppdatertTidspunkt()
        sendVedtaksperiodeForkastet()
        assertEquals(tidspunktVedFerdigstilling, oppgaveOppdatertTidspunkt())

    }

    private fun sendVedtaksperiodeForkastet() {
        @Language("JSON")
        val json = """
{
    "@event_name": "vedtaksperiode_forkastet",
    "@id": "${UUID.randomUUID()}",
    "fødselsnummer": "$FØDSELSNUMMER",
    "vedtaksperiodeId": "$VEDTAKSPERIODE_ID"
}"""

        testRapid.sendTestMessage(json)
    }

    private fun oppgaveOppdatertTidspunkt() =
        sessionOf(dataSource).use  { session ->
            session.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                it.localDateTime("oppdatert")
            }.asSingle)
        }

    private fun vedtaksperiodeTilGodkjenning(): UUID {
        val godkjenningsmeldingId1 = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId1, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1
        )

        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erDigital = true
        )

        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1, 1 // Dette trigger manuell saksbehandling
        )

        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        return godkjenningsmeldingId1
    }
}
