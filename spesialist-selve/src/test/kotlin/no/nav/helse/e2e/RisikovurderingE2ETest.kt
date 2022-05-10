package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

private class RisikovurderingE2ETest : AbstractE2ETest() {

    private val funn1 = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivele = "ny sjekk ikke ok",
        kreverSupersaksbehandler = true
    ))

    private val funn2 = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivele = "8-4 ikke ok",
        kreverSupersaksbehandler = false
    ))

    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        godkjenningsoppgave(funn1, VEDTAKSPERIODE_ID)

        assertOppgaveType("RISK_QA", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        godkjenningsoppgave(funn2, VEDTAKSPERIODE_ID)

        assertOppgaveType("SØKNAD", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `Venter på alle løsninger på utstedte risikobehov`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        godkjenningsoppgave(
            funn = funn2,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            aktiveVedtaksperioder = listOf(
                Testmeldingfabrikk.AktivVedtaksperiodeJson(
                    ORGNR,
                    VEDTAKSPERIODE_ID,
                    Periodetype.FØRSTEGANGSBEHANDLING
                ),
                Testmeldingfabrikk.AktivVedtaksperiodeJson(
                    "456789123",
                    UUID.randomUUID(),
                    Periodetype.FØRSTEGANGSBEHANDLING
                ),
                Testmeldingfabrikk.AktivVedtaksperiodeJson(
                    "789456123",
                    UUID.randomUUID(),
                    Periodetype.FØRSTEGANGSBEHANDLING
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
        funn: List<Risikofunn>,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        aktiveVedtaksperioder: List<Testmeldingfabrikk.AktivVedtaksperiodeJson> = listOf(
            Testmeldingfabrikk.AktivVedtaksperiodeJson(
                ORGNR,
                vedtaksperiodeId,
                Periodetype.FØRSTEGANGSBEHANDLING
            )
        ),
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
    ) {
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            aktiveVedtaksperioder = aktiveVedtaksperioder,
            utbetalingId = UTBETALING_ID
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
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
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
        Assertions.assertEquals(forventet, sessionOf(dataSource).use  {
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
