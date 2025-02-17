package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.spesialist.db.DatabaseIntegrationTest
import no.nav.helse.spesialist.db.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgVarselApiRepositoryTest: DatabaseIntegrationTest() {

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettGenerasjon(spleisBehandlingId = spleisBehandlingId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        nyttVarsel(kode = "EN_ANNEN_KODE", status = "INAKTIV", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)

        val varsler = apiVarselRepository.finnVarslerSomIkkeErInaktiveFor(PERIODE.id, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler for uberegnet periode`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettVarseldefinisjon(kode = "EN_TREDJE_KODE")

        val spleisBehandlingId2 = UUID.randomUUID()
        opprettGenerasjon(spleisBehandlingId = spleisBehandlingId2)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId2)
        nyttVarsel(kode = "EN_TREDJE_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId2, status = "INAKTIV")
        val varsler = apiVarselRepository.finnVarslerForUberegnetPeriode(PERIODE.id)

        assertTrue(varsler.isNotEmpty())
        assertEquals(2, varsler.size)
    }

    @Test
    fun `varsel er aktivt`() {
        val spleisBehandlingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettVarseldefinisjon(kode = "EN_KODE")
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)

        val generasjonId = finnGenerasjonId(spleisBehandlingId)
        assertEquals(true, apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er ikke aktivt - vurdert`() {
        val spleisBehandlingId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        val generasjonId = finnGenerasjonId(spleisBehandlingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        apiVarselRepository.settStatusVurdert(generasjonId = generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        assertEquals(false, apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er ikke aktivt - godkjent`() {
        val spleisBehandlingId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettOppgave()
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        val generasjonId = finnGenerasjonId(spleisBehandlingId)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselRepository.godkjennVarslerFor(oppgaveId)
        assertEquals(false, apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er godkjent`() {
        val spleisBehandlingId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettOppgave()
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        val generasjonId = finnGenerasjonId(spleisBehandlingId)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselRepository.godkjennVarslerFor(oppgaveId)
        assertEquals(true, apiVarselRepository.erGodkjent("EN_KODE", generasjonId))
    }

    @Test
    fun `erGodkjent returnerer null hvis varsel ikke finnes`() {
        val spleisBehandlingId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        opprettGenerasjon(vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        val generasjonId = finnGenerasjonId(spleisBehandlingId)
        assertNull(apiVarselRepository.erGodkjent("EN_KODE", generasjonId))
    }

    @Test
    fun `erAktiv returnerer null hvis varsel ikke finnes`() {
        val spleisBehandlingId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        opprettGenerasjon(vedtaksperiodeId = PERIODE.id, spleisBehandlingId = spleisBehandlingId)
        val generasjonId = finnGenerasjonId(spleisBehandlingId)
        assertNull(apiVarselRepository.erAktiv("EN_KODE", generasjonId))
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
        opprettPerson()
        opprettArbeidsgiver()
        with(uberegnetPeriode) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }

        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom)
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
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(fom = uberegnetPeriode.fom, tom = uberegnetPeriode.tom, utbetalingId = UUID.randomUUID())
        opprettVedtaksperiode(
            vedtaksperiodeId = periodeMedOppgave.id,
            fom = periodeMedOppgave.fom,
            tom = periodeMedOppgave.tom,
            spleisBehandlingId = UUID.randomUUID()
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
        opprettPerson()
        opprettArbeidsgiver()
        with(uberegnetPeriode) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom)
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
        opprettPerson()
        opprettArbeidsgiver()
        with(uberegnetPeriode) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(beregnetPeriode) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom)
            )
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode.fom)
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
        opprettPerson()
        opprettArbeidsgiver()
        with(uberegnetPeriode1) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(uberegnetPeriode2) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(
                vedtaksperiodeId = id,
                fom = fom,
                tom = tom,
                utbetalingId = UUID.randomUUID(),
                spleisBehandlingId = spleisBehandlingId
            )
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode1.fom)
            )
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode1.fom)
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
        opprettPerson()
        opprettArbeidsgiver()
        with(uberegnetPeriode1) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(uberegnetPeriode2) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
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
        opprettPerson()
        opprettArbeidsgiver()
        with(uberegnetPeriode1) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(uberegnetPeriode2) {
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, utbetalingId = UUID.randomUUID())
        }
        with(periodeMedOppgave) {
            val spleisBehandlingId = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = id, fom = fom, tom = tom, spleisBehandlingId = spleisBehandlingId)
            oppdaterBehandlingdata(
                SpleisVedtaksperiode(vedtaksperiodeId = id, spleisBehandlingId, fom, tom, uberegnetPeriode2.fom)
            )
            opprettOppgave(vedtaksperiodeId = id)
        }

        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode2.id), perioderSomSkalViseVarsler)
    }

    private fun finnGenerasjonId(spleisBehandlingId: UUID) = dbQuery.single(
        "select unik_id from behandling where behandling.spleis_behandling_id = :spleisBehandlingId",
        "spleisBehandlingId" to spleisBehandlingId
    ) { it.uuid("unik_id") }

    private fun periode(fom: LocalDate, tom: LocalDate) = Periode(UUID.randomUUID(), fom, tom)

}
