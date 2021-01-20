package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.modell.overstyring.Dagtype
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.vedtak.snapshot.ArbeidsgiverFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class OverstyringE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SNAPSHOTV1 = "{}"
    }

    @Test
    fun `saksbehandler overstyrer sykdomstidslinje`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1
        val hendelseId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = hendelseId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(hendelseId, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = hendelseId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = hendelseId
        )
        assertSaksbehandlerOppgaveOpprettet(hendelseId)
        sendOverstyrteDager(
            ORGNR, SAKSBEHANDLER_EPOST, listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )

        assertTrue(overstyringDao.finnOverstyring(FØDSELSNUMMER, ORGNR).isNotEmpty())
        assertTrue(oppgaveDao.finnOppgaver().none { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) })

        val hendelseId2 = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )
        sendEgenAnsattløsning(hendelseId2, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = hendelseId2,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = hendelseId2
        )
        val oppgave = oppgaveDao.finnOppgaver().find { it.fødselsnummer == FØDSELSNUMMER }
        assertNotNull(oppgave)
        assertEquals(SAKSBEHANDLER_EPOST, oppgave.saksbehandlerepost)
    }

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        val hendelseId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns objectMapper.writeValueAsString(
            PersonFraSpleisDto(
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                arbeidsgivere = listOf(
                    ArbeidsgiverFraSpleisDto(
                        organisasjonsnummer = ORGNR,
                        id = hendelseId,
                        vedtaksperioder = emptyList()
                    )
                ),
                inntektsgrunnlag = objectMapper.nullNode()
            )
        )
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = hendelseId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("Bransje1", "Bransje2")
        )
        sendEgenAnsattløsning(hendelseId, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = hendelseId,
            erDigital = true
        )
        sendOverstyrteDager(
            ORGNR, SAKSBEHANDLER_EPOST, listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )

        val hendelseId2 = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )
        sendEgenAnsattløsning(hendelseId2, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = hendelseId2,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = hendelseId2
        )

        // TODO: bør ikke koble seg på daoer i E2E
        assertTrue(oppgaveDao.finnOppgaver().any { it.fødselsnummer == FØDSELSNUMMER })

        val snapshot = vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER)
        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere.first().overstyringer
        assertEquals(1, overstyringer.size)
        assertEquals(1, overstyringer.first().overstyrteDager.size)
    }

    private fun assertSaksbehandlerOppgaveOpprettet(hendelseId: UUID) {
        val saksbehandlerOppgaver = oppgaveDao.finnOppgaver()
        assertEquals(
            1,
            saksbehandlerOppgaver.filter { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) }.size
        )
        assertTrue(saksbehandlerOppgaver.any { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) })
    }
}
