package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private class RisikovurderingE2ETest : AbstractE2ETest() {

    private val funn1 = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivelse = "ny sjekk ikke ok",
        kreverSupersaksbehandler = true
    ))

    private val funn2 = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivelse = "8-4 ikke ok",
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
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
    ) {
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, vedtaksperiodeId)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            ekstraArbeidsgivere = ekstraArbeidsgivere
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId,
            kanGodkjennesAutomatisk = false,
            funn = funn
        )
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
