package no.nav.helse.modell.oppgave.behandlingsstatistikk

import DatabaseIntegrationTest
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.spesialist.api.vedtaksperiode.Mottakertype
import org.junit.jupiter.api.Assertions.assertTrue
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkType as BehandlingsstatistikkTypeForApi

internal class BehandlingsstatistikkDaoTest : DatabaseIntegrationTest() {

    private val NOW = LocalDate.now()

    @Test
    fun `henter automatiserte UTS-saker`() {
        automatisertSakForGittMottaker(mottakertype = Mottakertype.SYKMELDT)
    }

    @Test
    fun `henter automatiserte kun refusjon-saker`() {
        automatisertSakForGittMottaker(mottakertype = Mottakertype.ARBEIDSGIVER)
    }

    @Test
    fun `henter automatiserte delvis refusjon-saker`() {
        automatisertSakForGittMottaker(mottakertype = Mottakertype.BEGGE)
    }

    private fun automatisertSakForGittMottaker(mottakertype: Mottakertype) {
        nyPersonMedAutomatiskVedtak(mottakertype = mottakertype)
        val (perInntekttype, perPeriodetype, perMottakertype) =
            behandlingsstatistikkDao.getAutomatiseringerPerInntektOgPeriodetype(NOW)
        assertEquals(1, perInntekttype[no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER])
        assertEquals(1, perPeriodetype[no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING])
        assertEquals(1, perMottakertype[mottakertype])
    }
    @Test
    fun `henter statikk for tilgjengelige oppgaver`() {
        nyPerson()
        val dto = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerInntektOgPeriodetype()
        assertEquals(1, dto.perInntekttype[no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER])
        assertEquals(0, dto.perInntekttype[no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde.FLERE_ARBEIDSGIVERE])
        assertEquals(1, dto.perPeriodetype[no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING])
        assertTrue(dto.perMottakertype.isEmpty())
    }


    @Test
    fun `en periode til godkjenning`() {
        nyPerson()
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(0, dto.fullførteBehandlinger.totalt)
        assertEquals(0, dto.fullførteBehandlinger.automatisk)
        assertEquals(0, dto.fullførteBehandlinger.manuelt.totalt)
        assertEquals(0, dto.fullførteBehandlinger.annullert)
        assertEquals(0, dto.tildelteOppgaver.totalt)
        assertEquals(0, dto.tildelteOppgaver.perPeriodetype.size)
        assertEquals(1, dto.oppgaverTilGodkjenning.totalt)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype.size)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype[BehandlingsstatistikkTypeForApi.FØRSTEGANGSBEHANDLING])
    }

    @Test
    fun `antall tildelte oppgaver`() {
        nyPerson()
        opprettSaksbehandler()
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLER_OID)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.tildelteOppgaver.totalt)
        assertEquals(1, dto.tildelteOppgaver.perPeriodetype[BehandlingsstatistikkTypeForApi.FØRSTEGANGSBEHANDLING])
    }

    @Test
    fun antallManuelleGodkjenninger() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, Oppgavestatus.Ferdigstilt)
        assertTrue(behandlingsstatistikkDao.getManueltUtførteOppgaverPerInntektOgPeriodetype(NOW).perMottakertype.isEmpty())
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.fullførteBehandlinger.totalt)
        assertEquals(1, dto.fullførteBehandlinger.manuelt.totalt)
        assertEquals(0, dto.fullførteBehandlinger.automatisk)
        assertEquals(0, dto.fullførteBehandlinger.annullert)
    }

    @Test
    fun antallAutomatiskeGodkjenninger() {
        nyPersonMedAutomatiskVedtak()
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.fullførteBehandlinger.totalt)
        assertEquals(0, dto.fullførteBehandlinger.manuelt.totalt)
        assertEquals(1, dto.fullførteBehandlinger.automatisk)
        assertEquals(0, dto.fullførteBehandlinger.annullert)
    }

    @Test
    fun antallAnnulleringer() {
        opprettSaksbehandler()
        utbetalingDao.nyAnnullering(LocalDateTime.now(), SAKSBEHANDLER_OID)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.fullførteBehandlinger.totalt)
        assertEquals(0, dto.fullførteBehandlinger.manuelt.totalt)
        assertEquals(1, dto.fullførteBehandlinger.annullert)
        assertEquals(0, dto.fullførteBehandlinger.automatisk)
    }

    @Test
    fun `flere periodetyper`() {
        nyPerson()
        nyVedtaksperiode(Periodetype.FORLENGELSE)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(2, dto.oppgaverTilGodkjenning.totalt)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype[BehandlingsstatistikkTypeForApi.FØRSTEGANGSBEHANDLING])
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype[BehandlingsstatistikkTypeForApi.FORLENGELSE])
    }

    @Test
    fun `tar ikke med innslag som er eldre enn dato som sendes inn for fullførte behandlinger`() {
        nyPersonMedAutomatiskVedtak()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)
        val fremtidigDato = NOW.plusDays(1)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(fremtidigDato)
        assertEquals(0, dto.fullførteBehandlinger.totalt)
        assertEquals(0, dto.fullførteBehandlinger.annullert)
        assertEquals(0, dto.fullførteBehandlinger.manuelt.totalt)
        assertEquals(0, dto.fullførteBehandlinger.automatisk)
        assertEquals(1, dto.oppgaverTilGodkjenning.totalt)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype.size)
    }

    private operator fun List<Pair<BehandlingsstatistikkTypeForApi, Int>>.get(type: BehandlingsstatistikkTypeForApi) = this.first { it.first == type }.second

    private fun nyPersonMedAutomatiskVedtak(
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        mottakertype: Mottakertype = Mottakertype.ARBEIDSGIVER,
    ) {
        godkjenningsbehov()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(periodetype = periodetype, inntektskilde = inntektskilde)
        nyttAutomatiseringsinnslag(true)
        when (mottakertype) {
            Mottakertype.ARBEIDSGIVER -> utbetalingTilArbeidsgiver()
            Mottakertype.SYKMELDT -> utbetalingTilPerson()
            Mottakertype.BEGGE -> utbetalingTilBegge()
        }
    }

    private fun utbetalingTilArbeidsgiver() {
        val utbetaling_idId = utbetalingsopplegg(abeløp = 4000, pbeløp = 0)
    }

    private fun utbetalingTilPerson() {
        val utbetaling_idId = utbetalingsopplegg(abeløp = 0, pbeløp = 4000)
    }

    private fun utbetalingTilBegge() {
        val utbetaling_idId = utbetalingsopplegg(abeløp = 2000, pbeløp = 2000)
    }

    private fun utbetalingsopplegg(abeløp: Int, pbeløp: Int) {
        val arbeidsgiveroppdragId = lagArbeidsgiveroppdrag(fagsystemId())
        val personOppdragId = lagPersonoppdrag(fagsystemId())
        val utbetaling_idId = lagUtbetalingId(arbeidsgiveroppdragId, personOppdragId, UTBETALING_ID, arbeidsgiverbeløp = abeløp, personbeløp = pbeløp)
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId, Utbetalingsstatus.UTBETALT, LocalDateTime.now(), "{}")
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)
    }
}
