package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VarselServiceTest : DatabaseIntegrationTest() {

    @Test
    fun `Ingen periode med oppgave - uberegnet periode skal ikke vise varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-04", "2023-01-06", PERIODE.id)
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertFalse(perioderSomSkalViseVarsler.contains(vedtaksperiodeId))
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId = UUID.randomUUID()
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-04", "2023-01-06", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertEquals(setOf(vedtaksperiodeId), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal ikke vise varsler fordi den ikke er tilstøtende perioden med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId = UUID.randomUUID()
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-05", "2023-01-06", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertFalse(perioderSomSkalViseVarsler.contains(vedtaksperiodeId))
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave (over helg)`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId = UUID.randomUUID()
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-06", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-09", "2023-01-13", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertEquals(setOf(vedtaksperiodeId), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende de to andre periodene`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId = UUID.randomUUID()
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-01", "2023-01-02", vedtaksperiodeId)
        val beregnetPeriode1 = opprettBeregnetPeriode("2023-01-03", "2023-01-04")
        val beregnetPeriode2 = opprettBeregnetPeriode("2023-01-05", "2023-01-06", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode1, beregnetPeriode2, uberegnetPeriode))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertTrue(perioderSomSkalViseVarsler.contains(vedtaksperiodeId))
    }

    @Test
    fun `Begge uberegnede perioder skal vise varsler fordi de henger sammen med perioden med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val uberegnetPeriode1 = opprettUberegnetPeriode("2023-01-01", "2023-01-02", vedtaksperiodeId1)
        val uberegnetPeriode2 = opprettUberegnetPeriode("2023-01-03", "2023-01-04", vedtaksperiodeId2)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-05", "2023-01-06", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode1, uberegnetPeriode2))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertEquals(setOf(vedtaksperiodeId1, vedtaksperiodeId2), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Ingen uberegnede perioder skal vise varsler fordi de ikke henger sammen med perioden med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val uberegnetPeriode1 = opprettUberegnetPeriode("2023-01-01", "2023-01-02", vedtaksperiodeId1)
        val uberegnetPeriode2 = opprettUberegnetPeriode("2023-01-03", "2023-01-04", vedtaksperiodeId2)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-06", "2023-01-07", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode1, uberegnetPeriode2))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertFalse(perioderSomSkalViseVarsler.any(setOf(vedtaksperiodeId1, vedtaksperiodeId2)::contains))
    }

    @Test
    fun `Bare den tilstøtende uberegnede perioden skal vise varsler`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val uberegnetPeriode1 = opprettUberegnetPeriode("2023-01-01", "2023-01-02", vedtaksperiodeId1)
        val uberegnetPeriode2 = opprettUberegnetPeriode("2023-01-04", "2023-01-05", vedtaksperiodeId2)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-06", "2023-01-07", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode1, uberegnetPeriode2))
        val perioderSomSkalViseVarsler = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon, oppgaveApiDao)

        assertEquals(setOf(vedtaksperiodeId2), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Test med flere generasjoner - forventer at begge de uberegnede skal vise varsler`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeId = UUID.randomUUID()
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-04", "2023-01-05", PERIODE.id)
        val snapshotGenerasjon1 = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val snapshotGenerasjon2 = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val perioderSomSkalViseVarsler1 = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon1, oppgaveApiDao)
        val perioderSomSkalViseVarsler2 = varselService.uberegnedePerioderSomSkalViseVarsler(snapshotGenerasjon2, oppgaveApiDao)

        assertEquals(setOf(vedtaksperiodeId), perioderSomSkalViseVarsler1)
        assertEquals(setOf(vedtaksperiodeId), perioderSomSkalViseVarsler2)
    }
}