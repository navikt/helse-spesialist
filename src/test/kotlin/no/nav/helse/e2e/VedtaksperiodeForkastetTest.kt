package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.snapshotUtenWarnings
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class VedtaksperiodeForkastetTest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private val SNAPSHOTV1 = snapshotUtenWarnings(VEDTAKSPERIODE_ID)
    }

    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `VedtaksperiodeForkastet oppdaterer ikke oppgave-tabellen dersom status er inaktiv`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        vedtaksperiodeTilGodkjenning()

        sendSaksbehandlerløsning(OPPGAVEID, "", "", UUID.randomUUID(), true)
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
    "fødselsnummer": "$UNG_PERSON_FNR_2018",
    "vedtaksperiodeId": "$VEDTAKSPERIODE_ID"
}"""

        testRapid.sendTestMessage(json)
    }

    private fun oppgaveOppdatertTidspunkt() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                it.localDateTime("oppdatert")
            }.asSingle)
        }

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
