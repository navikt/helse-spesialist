package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgVarselApiRepositoryTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettBehandling(spleisBehandlingId = spleisBehandlingId, utbetalingId = utbetalingId, fødselsnummer = person.id.value)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        nyttVarsel(kode = "EN_ANNEN_KODE", status = "INAKTIV", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)

        val varsler = apiVarselRepository.finnVarslerSomIkkeErInaktiveFor(PERIODE.id, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler for uberegnet periode`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettVarseldefinisjon(kode = "EN_TREDJE_KODE")

        val spleisBehandlingId2 = UUID.randomUUID()
        opprettBehandling(spleisBehandlingId = spleisBehandlingId2, fødselsnummer = person.id.value)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId2)
        nyttVarsel(kode = "EN_TREDJE_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId2, status = "INAKTIV")
        val varsler = apiVarselRepository.finnVarslerForUberegnetPeriode(PERIODE.id)

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
        val uberegnetPeriode = periode(2 jan 2023, 3 jan 2023)
        val periodeMedOppgave = periode(4 jan 2023, 5 jan 2023)
        opprettArbeidsgiver()
        with(uberegnetPeriode) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }

        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom),
                fødselsnummer = person.id.value,
            )
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal ikke vise varsler når den ikke er tilstøtende perioden med oppgave`() {
        val uberegnetPeriode = periode(2 jan 2023, 3 jan 2023)
        val periodeMedOppgave = periode(5 jan 2023, 6 jan 2023)
        opprettArbeidsgiver()
        opprettVedtaksperiode(fom = uberegnetPeriode.fom, tom = uberegnetPeriode.tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        opprettVedtaksperiode(
            vedtaksperiodeId = periodeMedOppgave.id,
            fom = periodeMedOppgave.fom,
            tom = periodeMedOppgave.tom,
            spleisBehandlingId = UUID.randomUUID(),
            fødselsnummer = person.id.value,
        )
        opprettOppgave(vedtaksperiodeId = periodeMedOppgave.id)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertFalse(perioderSomSkalViseVarsler.contains(uberegnetPeriode.id))
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave (over helg)`() {
        val uberegnetPeriode = periode(2 jan 2023, 6 jan 2023)
        val periodeMedOppgave = periode(9 jan 2023, 13 jan 2023)
        opprettArbeidsgiver()
        with(uberegnetPeriode) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom),
                fødselsnummer = person.id.value,
            )
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)
        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende de to andre periodene`() {
        val uberegnetPeriode = periode(1 jan 2023, 2 jan 2023)
        val beregnetPeriode = periode(3 jan 2023, 4 jan 2023)
        val periodeMedOppgave = periode(5 jan 2023, 6 jan 2023)
        opprettArbeidsgiver()
        with(uberegnetPeriode) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(beregnetPeriode) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom),
                fødselsnummer = person.id.value,
            )
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom),
                fødselsnummer = person.id.value,
            )
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)
        assertEquals(setOf(periodeMedOppgave.id, beregnetPeriode.id, uberegnetPeriode.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Begge uberegnede perioder skal vise varsler fordi de henger sammen med perioden med oppgave`() {
        val uberegnetPeriode1 = periode(1 jan 2023, 2 jan 2023)
        val uberegnetPeriode2 = periode(3 jan 2023, 4 jan 2023)
        val periodeMedOppgave = periode(5 jan 2023, 6 jan 2023)
        opprettArbeidsgiver()
        with(uberegnetPeriode1) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(uberegnetPeriode2) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(
                vedtaksperiodeId = id,
                fom = fom,
                tom = tom,
                utbetalingId = UUID.randomUUID(),
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = person.id.value,
            )
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode1.fom),
                fødselsnummer = person.id.value,
            )
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode1.fom),
                fødselsnummer = person.id.value,
            )
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)
        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode1.id, uberegnetPeriode2.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Ingen uberegnede perioder skal vise varsler fordi de ikke henger sammen med perioden med oppgave`() {
        val uberegnetPeriode1 = periode(1 jan 2023, 2 jan 2023)
        val uberegnetPeriode2 = periode(3 jan 2023, 4 jan 2023)
        val periodeMedOppgave = periode(6 jan 2023, 7 jan 2023)
        opprettArbeidsgiver()
        with(uberegnetPeriode1) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(uberegnetPeriode2) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)
        assertFalse(perioderSomSkalViseVarsler.any(setOf(uberegnetPeriode1.id, uberegnetPeriode2.id)::contains))
    }

    @Test
    fun `Bare den tilstøtende uberegnede perioden skal vise varsler`() {
        val uberegnetPeriode1 = periode(1 jan 2023, 2 jan 2023)
        val uberegnetPeriode2 = periode(3 jan 2023, 4 jan 2023)
        val periodeMedOppgave = periode(5 jan 2023, 6 jan 2023)
        opprettArbeidsgiver()
        with(uberegnetPeriode1) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(uberegnetPeriode2) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID(), fødselsnummer = person.id.value)
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode2.fom),
                fødselsnummer = person.id.value,
            )
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode2.id), perioderSomSkalViseVarsler)
    }

    private fun periode(
        fom: LocalDate,
        tom: LocalDate,
    ) = Periode(UUID.randomUUID(), fom, tom)
}
