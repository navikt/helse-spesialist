package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.BehandlingsstatistikkDao.StatistikkPerKombinasjon.Mottakertype
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.db.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate

@Isolated
internal class PgBehandlingsstatistikkDaoTest : DatabaseIntegrationTest() {

    private val NOW = LocalDate.now()

    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("truncate oppgave, automatisering, annullert_av_saksbehandler, totrinnsvurdering cascade")
    }

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

    @Test
    fun `henter statikk for tilgjengelige oppgaver`() {
        nyPerson()
        val dto = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerInntektOgPeriodetype()
        assertEquals(1, dto.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER])
        assertEquals(0, dto.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE])
        assertEquals(1, dto.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING])
        assertTrue(dto.perMottakertype.isEmpty())
    }

    @Test
    fun `hent antall tilgjengelige oppgaver for gitt egenskap`() {
        nyPerson()
        val antall = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.SØKNAD)
        assertEquals(1, antall)
    }

    @Test
    fun `hent antall ferdigstilte oppgaver for gitt egenskap`() {
        nyPerson()
        oppgaveDao.updateOppgave(OPPGAVE_ID, oppgavestatus = "Ferdigstilt", egenskaper = listOf(EgenskapForDatabase.SØKNAD))
        val antall = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.SØKNAD, LocalDate.now())
        assertEquals(1, antall)
    }

    @Test
    fun`Får antall tilgjengelige beslutteroppgaver`() {
        nyPerson()
        opprettSaksbehandler()
        oppgaveDao.updateOppgave(
            oppgaveId = OPPGAVE_ID,
            oppgavestatus = Oppgavestatus.AvventerSaksbehandler.toString(),
            egenskaper = listOf(EGENSKAP, EgenskapForDatabase.BESLUTTER)
        )
        assertEquals(0, behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver())
        opprettTotrinnsvurdering(saksbehandlerOid = SAKSBEHANDLER_OID)
        assertEquals(1, behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver())
        oppgaveDao.updateOppgave(
            oppgaveId = OPPGAVE_ID,
            oppgavestatus = Oppgavestatus.Ferdigstilt.toString(),
            egenskaper = listOf(EGENSKAP, EgenskapForDatabase.BESLUTTER)
        )
        assertEquals(0, behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver())
    }

    @Test
    fun`Får antall fullførte beslutteroppgaver`() {
        nyPerson()
        opprettSaksbehandler()
        utbetalingsopplegg(1000, 0)
        assertEquals(0, behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(LocalDate.now().minusDays(1)))
        opprettTotrinnsvurdering(saksbehandlerOid = SAKSBEHANDLER_OID, ferdigstill = true)
        assertEquals(1, behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(LocalDate.now().minusDays(1)))
    }

    @Test
    fun`Får antall automatiserte revurderinger`() {
        nyPersonMedAutomatiskVedtak(utbetalingtype = Utbetalingtype.REVURDERING)
        assertEquals(1, behandlingsstatistikkDao.getAutomatiseringPerKombinasjon(LocalDate.now()).perUtbetalingtype[BehandlingsstatistikkDao.StatistikkPerKombinasjon.Utbetalingtype.REVURDERING])
    }

    @Test
    fun`Får antall tilgjengelige egen ansatt-oppgaver`() {
        nyPerson()
        opprettSaksbehandler()
        oppgaveDao.updateOppgave(
            oppgaveId = OPPGAVE_ID,
            oppgavestatus = Oppgavestatus.AvventerSaksbehandler.toString(),
            egenskaper = listOf(EGENSKAP, EgenskapForDatabase.EGEN_ANSATT)
        )
        assertEquals(1, behandlingsstatistikkDao.getAntallTilgjengeligeEgenAnsattOppgaver())
        oppgaveDao.updateOppgave(
            oppgaveId = OPPGAVE_ID,
            oppgavestatus = Oppgavestatus.Ferdigstilt.toString(),
            egenskaper = listOf(EGENSKAP, EgenskapForDatabase.EGEN_ANSATT)
        )
        assertEquals(0, behandlingsstatistikkDao.getAntallTilgjengeligeEgenAnsattOppgaver())
    }

    @Test
    fun`Får antall fullførte egen ansatt-oppgaver`() {
        nyPerson()
        opprettSaksbehandler()
        oppgaveDao.updateOppgave(
            oppgaveId = OPPGAVE_ID,
            oppgavestatus = Oppgavestatus.Ferdigstilt.toString(),
            egenskaper = listOf(EGENSKAP, EgenskapForDatabase.EGEN_ANSATT)
        )
        assertEquals(1, behandlingsstatistikkDao.getAntallManueltFullførteEgenAnsattOppgaver(LocalDate.now().minusDays(1)))
    }

    private fun automatisertSakForGittMottaker(mottakertype: Mottakertype) {
        nyPersonMedAutomatiskVedtak(mottakertype = mottakertype)
        val (perInntekttype, perPeriodetype, perMottakertype) =
            behandlingsstatistikkDao.getAutomatiseringPerKombinasjon(NOW)
        assertEquals(1, perInntekttype[Inntektskilde.EN_ARBEIDSGIVER])
        assertEquals(1, perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING])
        assertEquals(1, perMottakertype[mottakertype])
    }

    private fun nyPersonMedAutomatiskVedtak(
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        mottakertype: Mottakertype = Mottakertype.ARBEIDSGIVER,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING
    ) {
        godkjenningsbehov()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(periodetype = periodetype, inntektskilde = inntektskilde)
        nyttAutomatiseringsinnslag(true)
        when (mottakertype) {
            Mottakertype.ARBEIDSGIVER -> utbetalingTilArbeidsgiver(utbetalingtype)
            Mottakertype.SYKMELDT -> utbetalingTilPerson(utbetalingtype)
            Mottakertype.BEGGE -> utbetalingTilBegge(utbetalingtype)
        }
    }

    private fun utbetalingTilArbeidsgiver(utbetalingtype: Utbetalingtype) = utbetalingsopplegg(beløpTilArbeidsgiver = 4000, beløpTilSykmeldt = 0, utbetalingtype = utbetalingtype)

    private fun utbetalingTilPerson(utbetalingtype: Utbetalingtype) = utbetalingsopplegg(beløpTilArbeidsgiver = 0, beløpTilSykmeldt = 4000, utbetalingtype = utbetalingtype)

    private fun utbetalingTilBegge(utbetalingtype: Utbetalingtype) = utbetalingsopplegg(beløpTilArbeidsgiver = 2000, beløpTilSykmeldt = 2000, utbetalingtype = utbetalingtype)
}
