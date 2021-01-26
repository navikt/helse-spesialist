package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.snapshotUtenWarnings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

private class RisikovurderingE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
    }

    @BeforeEach
    fun setup() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        every { miljøstyrtFeatureToggle.automatisering() }.returns(true)
    }

    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        godkjenningsoppgave(mapOf("ny sjekk ikke ok" to true), VEDTAKSPERIODE_ID)

        assertOppgaveType("RISK_QA", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        godkjenningsoppgave(mapOf("8-4 ikke ok" to false), VEDTAKSPERIODE_ID)

        assertOppgaveType("SØKNAD", VEDTAKSPERIODE_ID)
    }

    fun godkjenningsoppgave(funn: Map<String, Boolean>, vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID) {
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            vedtaksperiodeId
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, vedtaksperiodeId)
        sendArbeidsgiverinformasjonløsning(
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
            vedtaksperiodeId = vedtaksperiodeId,
            kanGodkjennesAutomatisk = false,
            funn = funn
        )
    }

    private fun assertOppgaveType(forventet: String, vedtaksperiodeId: UUID) {
        Assertions.assertEquals(forventet, using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT type FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("type") }.asSingle
            )
        })
    }
}
