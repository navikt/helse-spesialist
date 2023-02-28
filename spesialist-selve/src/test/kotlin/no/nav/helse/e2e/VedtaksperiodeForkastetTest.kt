package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendUtbetalingEndret
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetTest : AbstractE2ETest() {
    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `VedtaksperiodeForkastet oppdaterer ikke oppgave-tabellen dersom status er inaktiv`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot()
        vedtaksperiodeTilGodkjenning()

        sendSaksbehandlerløsningFraAPI(OPPGAVEID, "", "", UUID.randomUUID(), true)
        sendUtbetalingEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            utbetalingId = UTBETALING_ID,
            type = "UTBETALING",
            status = UTBETALT,
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID"
        )
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
            AKTØR,
            FØDSELSNUMMER,
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId1, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId1,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId1,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1
        )

        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1, 1 // Dette trigger manuell saksbehandling
        )

        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        return godkjenningsmeldingId1
    }
}
