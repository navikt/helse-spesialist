package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgVarselApiRepositoryTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val utbetalingId = UtbetalingId(UUID.randomUUID())

        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode, utbetalingId = utbetalingId)

        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")

        opprettVarsel(behandling = behandling, "EN_KODE")
        val varsel2 = opprettVarsel(behandling = behandling, "EN_ANNEN_KODE")
        varsel2.deaktiver()
        sessionContext.varselRepository.lagre(varsel2)

        val varsler = apiVarselRepository.finnVarslerSomIkkeErInaktiveFor(vedtaksperiode.id.value, utbetalingId.value)

        assertTrue(varsler.isNotEmpty())
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler for uberegnet periode`() {
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode, utbetalingId = null)
        val behandling2 = opprettBehandling(vedtaksperiode)
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettVarseldefinisjon(kode = "EN_TREDJE_KODE")

        opprettVarsel(behandling = behandling2, "EN_KODE")
        opprettVarsel(behandling = behandling2, "EN_ANNEN_KODE")
        val varsel3 = opprettVarsel(behandling = behandling2, "EN_TREDJE_KODE")
        varsel3.deaktiver()
        sessionContext.varselRepository.lagre(varsel3)

        val varsler = apiVarselRepository.finnVarslerForUberegnetPeriode(vedtaksperiode.id.value)

        assertTrue(varsler.isNotEmpty())
        assertEquals(2, varsler.size)
    }

    @Test
    fun `Ingen periode med oppgave - uberegnet periode skal ikke vise varsler`() {
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(null)
        assertTrue(perioderSomSkalViseVarsler.isEmpty())
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave`() {
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode1, fom = 2 jan 2023, tom = 3 jan 2023, utbetalingId = null)
        val behandling = opprettBehandling(vedtaksperiode2, fom = 4 jan 2023, tom = 5 jan 2023, skjæringstidspunkt = 2 jan 2023, utbetalingId = UtbetalingId(UUID.randomUUID()))
        val oppgave = opprettOppgave(vedtaksperiode2, behandling)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)

        assertEquals(setOf(vedtaksperiode2.id.value, vedtaksperiode1.id.value), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal ikke vise varsler når den ikke er tilstøtende perioden med oppgave`() {
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode1, fom = 2 jan 2023, tom = 3 jan 2023, utbetalingId = null)
        val behandling = opprettBehandling(vedtaksperiode2, fom = 5 jan 2023, tom = 6 jan 2023, skjæringstidspunkt = 5 jan 2023, utbetalingId = UtbetalingId(UUID.randomUUID()))
        val oppgave = opprettOppgave(vedtaksperiode2, behandling)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)

        assertFalse(perioderSomSkalViseVarsler.contains(vedtaksperiode1.id.value))
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave (over helg)`() {
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode1, fom = 2 jan 2023, tom = 6 jan 2023, utbetalingId = null)
        val behandling = opprettBehandling(vedtaksperiode2, fom = 9 jan 2023, tom = 13 jan 2023, skjæringstidspunkt = 2 jan 2023, utbetalingId = UtbetalingId(UUID.randomUUID()))
        val oppgave = opprettOppgave(vedtaksperiode2, behandling)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)
        assertEquals(setOf(vedtaksperiode2.id.value, vedtaksperiode1.id.value), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende de to andre periodene`() {
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode3 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling1 = opprettBehandling(vedtaksperiode1, fom = 1 jan 2023, tom = 2 jan 2023, utbetalingId = null)
        opprettBehandling(vedtaksperiode2, fom = 3 jan 2023, tom = 4 jan 2023, skjæringstidspunkt = behandling1.skjæringstidspunkt, utbetalingId = UtbetalingId(UUID.randomUUID()))
        val behandling3 = opprettBehandling(vedtaksperiode3, fom = 5 jan 2023, tom = 6 jan 2023, skjæringstidspunkt = behandling1.skjæringstidspunkt, utbetalingId = UtbetalingId(UUID.randomUUID()))
        val oppgave = opprettOppgave(vedtaksperiode3, behandling3)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)
        assertEquals(setOf(vedtaksperiode3.id.value, vedtaksperiode2.id.value, vedtaksperiode1.id.value), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Begge uberegnede perioder skal vise varsler fordi de henger sammen med perioden med oppgave`() {
        val uberegnetPeriode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val uberegnetPeriode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val periodeMedOppgave = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling1 = opprettBehandling(uberegnetPeriode1, fom = 1 jan 2023, tom = 2 jan 2023)
        opprettBehandling(uberegnetPeriode2, fom = 3 jan 2023, tom = 4 jan 2023, skjæringstidspunkt = behandling1.skjæringstidspunkt)
        val behandling3 = opprettBehandling(periodeMedOppgave, fom = 5 jan 2023, tom = 6 jan 2023, skjæringstidspunkt = behandling1.skjæringstidspunkt)
        val oppgave = opprettOppgave(periodeMedOppgave, behandling3)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)
        assertEquals(setOf(periodeMedOppgave.id.value, uberegnetPeriode1.id.value, uberegnetPeriode2.id.value), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Ingen uberegnede perioder skal vise varsler fordi de ikke henger sammen med perioden med oppgave`() {
        val uberegnetPeriode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val uberegnetPeriode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val periodeMedOppgave = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(uberegnetPeriode1, fom = 1 jan 2023, tom = 2 jan 2023)
        opprettBehandling(uberegnetPeriode2, fom = 3 jan 2023, tom = 4 jan 2023)
        val behandling3 = opprettBehandling(periodeMedOppgave, fom = 6 jan 2023, tom = 7 jan 2023)
        val oppgave = opprettOppgave(periodeMedOppgave, behandling3)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)
        assertFalse(perioderSomSkalViseVarsler.any(setOf(uberegnetPeriode1.id.value, uberegnetPeriode2.id.value)::contains))
    }

    @Test
    fun `Bare den tilstøtende uberegnede perioden skal vise varsler`() {
        val uberegnetPeriode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val uberegnetPeriode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val periodeMedOppgave = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(uberegnetPeriode1, fom = 1 jan 2023, tom = 2 jan 2023)
        val behandling2 = opprettBehandling(uberegnetPeriode2, fom = 3 jan 2023, tom = 4 jan 2023)
        val behandling3 = opprettBehandling(periodeMedOppgave, fom = 5 jan 2023, tom = 6 jan 2023, skjæringstidspunkt = behandling2.skjæringstidspunkt)
        val oppgave = opprettOppgave(periodeMedOppgave, behandling3)

        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgave.id)

        assertEquals(setOf(periodeMedOppgave.id.value, uberegnetPeriode2.id.value), perioderSomSkalViseVarsler)
    }
}
