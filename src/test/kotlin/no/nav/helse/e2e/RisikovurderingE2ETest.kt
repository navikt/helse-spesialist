package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.Toggles
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.snapshotUtenWarnings
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

private class RisikovurderingE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
    }

    @Language("json")
    private val funn1 = objectMapper.readTree(
        """
            [{
                "kategori": ["8-4"],
                "beskrivelse": "ny sjekk ikke ok",
                "kreverSupersaksbehandler": true
            }]
        """
    )
    private val funn2 = objectMapper.readTree(
        """
            [{
                "kategori": ["8-4"],
                "beskrivelse": "8-4 ikke ok",
                "kreverSupersaksbehandler": false
            }]
        """
    )

    @BeforeEach
    fun setup() {
        Toggles.Automatisering.enable()
    }

    @AfterEach
    fun teardown() {
        Toggles.Automatisering.pop()
    }

    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        godkjenningsoppgave(funn1, VEDTAKSPERIODE_ID)

        assertOppgaveType("RISK_QA", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        godkjenningsoppgave(funn2, VEDTAKSPERIODE_ID)

        assertOppgaveType("SØKNAD", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `Venter på alle løsninger på utstedte risikobehov`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        godkjenningsoppgave(
            funn = funn2,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            aktiveVedtaksperioder = listOf(
                Testmeldingfabrikk.AktivVedtaksperiodeJson(
                    ORGNR,
                    VEDTAKSPERIODE_ID,
                    Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
                ),
                Testmeldingfabrikk.AktivVedtaksperiodeJson(
                    "456789123",
                    UUID.randomUUID(),
                    Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
                ),
                Testmeldingfabrikk.AktivVedtaksperiodeJson(
                    "789456123",
                    UUID.randomUUID(),
                    Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
                ),
            ),
            ekstraArbeidsgivere = listOf(
                Testmeldingfabrikk.ArbeidsgiverinformasjonJson("456789123", "Shappa på hjørnet", listOf("Sjappe")),
                Testmeldingfabrikk.ArbeidsgiverinformasjonJson("789456123", "Borti der", listOf("Skredsøker"))
            )
        )

        assertOppgaveType("SØKNAD", VEDTAKSPERIODE_ID)
    }

    fun godkjenningsoppgave(
        funn: JsonNode,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        aktiveVedtaksperioder: List<Testmeldingfabrikk.AktivVedtaksperiodeJson> = listOf(
            Testmeldingfabrikk.AktivVedtaksperiodeJson(
                ORGNR,
                vedtaksperiodeId,
                Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
            )
        ),
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
    ) {
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            aktiveVedtaksperioder = aktiveVedtaksperioder
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, vedtaksperiodeId)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            ekstraArbeidsgivere = ekstraArbeidsgivere
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
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
        aktiveVedtaksperioder.forEach {
            sendRisikovurderingløsning(
                godkjenningsmeldingId = godkjenningsmeldingId,
                vedtaksperiodeId = it.vedtaksperiodeId,
                kanGodkjennesAutomatisk = false,
                funn = funn
            )
        }
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
