package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
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
import no.nav.helse.Meldingssender.sendSøknadSendt
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class RisikovurderingE2ETest : AbstractE2ETest() {

    private val funnSomKreverRiskTilgang = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivelse = "ny sjekk ikke ok",
        kreverSupersaksbehandler = true
    ))

    private val funnSomAlleKanBehandle = listOf(Risikofunn(
        kategori = listOf("8-4"),
        beskrivelse = "8-4 ikke ok",
        kreverSupersaksbehandler = false
    ))

    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        godkjenningsoppgave(funnSomKreverRiskTilgang, VEDTAKSPERIODE_ID)

        assertOppgaveType("RISK_QA", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        godkjenningsoppgave(funnSomAlleKanBehandle, VEDTAKSPERIODE_ID)

        assertOppgaveType("SØKNAD", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `sender med kunRefusjon`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns
                snapshot(personbeløp = 0, arbeidsgiverbeløp = 42, utbetalingId = UTBETALING_ID)

        godkjenningsoppgave(funn = emptyList())

        val riskbehov = testRapid.inspektør.hendelser("behov").first {
            it["@behov"].map(JsonNode::asText).contains("Risikovurdering")
        }
        assertTrue(riskbehov["Risikovurdering"]["kunRefusjon"].asBoolean())
    }

    fun godkjenningsoppgave(
        funn: List<Risikofunn>,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
    ) {
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
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

    private fun assertOppgaveType(forventet: String, vedtaksperiodeId: UUID) =
        assertEquals(forventet, sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT type FROM oppgave JOIN vedtak on vedtak.id = vedtak_ref WHERE vedtaksperiode_id = :vedtaksperiodeId",
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId)
                ).map { row -> row.string("type") }.asSingle
            )
        })
}
