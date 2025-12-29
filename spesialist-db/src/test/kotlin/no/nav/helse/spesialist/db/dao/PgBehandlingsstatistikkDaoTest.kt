package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.BehandlingsstatistikkDao.StatistikkPerKombinasjon.Mottakertype
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate
import java.util.UUID

@Isolated
internal class PgBehandlingsstatistikkDaoTest : AbstractDBIntegrationTest() {
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
        nyOppgaveForNyPerson()
        val dto = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerInntektOgPeriodetype()
        assertEquals(1, dto.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER])
        assertEquals(0, dto.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE])
        assertEquals(1, dto.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING])
        assertTrue(dto.perMottakertype.isEmpty())
    }

    @Test
    fun `hent antall tilgjengelige oppgaver for gitt egenskap`() {
        nyOppgaveForNyPerson()
        val antall = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.SØKNAD)
        assertEquals(1, antall)
    }

    @Test
    fun `hent antall ferdigstilte oppgaver for gitt egenskap`() {
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD))
            .avventSystemOgLagre(nyLegacySaksbehandler())
            .ferdigstillOgLagre()
        val antall = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.SØKNAD, LocalDate.now())
        assertEquals(1, antall)
    }

    @Test
    fun `Får antall tilgjengelige beslutteroppgaver`() {
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = nyLegacySaksbehandler()
        val beslutter = nyLegacySaksbehandler()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)

        assertEquals(0, behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver())

        nyTotrinnsvurdering(fødselsnummer, oppgave).sendTilBeslutterOgLagre(saksbehandlerWrapper = saksbehandler)
        oppgave.sendTilBeslutterOgLagre(beslutter = beslutter)
        assertEquals(1, behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver())

        oppgave
            .avventSystemOgLagre(saksbehandlerWrapper = beslutter)
            .ferdigstillOgLagre()
        assertEquals(0, behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver())
    }

    @Test
    fun `Får antall fullførte beslutteroppgaver`() {
        val fødselsnummer = opprettPerson().id.value
        val saksbehandler = nyLegacySaksbehandler()
        val beslutter = nyLegacySaksbehandler()
        val oppgave = nyOppgaveForNyPerson()
        assertEquals(0, behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(LocalDate.now().minusDays(1)))

        oppgave
            .sendTilBeslutterOgLagre(beslutter = beslutter)
            .ferdigstillOgLagre()
        nyTotrinnsvurdering(fødselsnummer, oppgave)
            .sendTilBeslutterOgLagre(saksbehandlerWrapper = saksbehandler)
            .ferdigstillOgLagre(beslutter = beslutter)

        assertEquals(1, behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(LocalDate.now().minusDays(1)))
    }

    @Test
    fun `Får antall automatiserte revurderinger`() {
        nyPersonMedAutomatiskVedtak(utbetalingtype = Utbetalingtype.REVURDERING)
        assertEquals(1, behandlingsstatistikkDao.getAutomatiseringPerKombinasjon(LocalDate.now()).perUtbetalingtype[BehandlingsstatistikkDao.StatistikkPerKombinasjon.Utbetalingtype.REVURDERING])
    }

    @Test
    fun `Får antall tilgjengelige egen ansatt-oppgaver`() {
        val oppgave = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.EGEN_ANSATT))
        assertEquals(1, behandlingsstatistikkDao.getAntallTilgjengeligeEgenAnsattOppgaver())
        oppgave
            .avventSystemOgLagre(nyLegacySaksbehandler())
            .ferdigstillOgLagre()
        assertEquals(0, behandlingsstatistikkDao.getAntallTilgjengeligeEgenAnsattOppgaver())
    }

    @Test
    fun `Får antall fullførte egen ansatt-oppgaver`() {
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.EGEN_ANSATT))
            .avventSystemOgLagre(nyLegacySaksbehandler())
            .ferdigstillOgLagre()

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
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
    ) {
        val person = opprettPerson()
        val hendelseId = UUID.randomUUID()
        godkjenningsbehov(fødselsnummer = person.id.value, hendelseId = hendelseId)
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver, periodetype = periodetype, inntektskilde = inntektskilde)
        val behandling = opprettBehandling(vedtaksperiode)
        nyttAutomatiseringsinnslag(true, vedtaksperiodeId = vedtaksperiode.id.value, utbetalingId = behandling.utbetalingId!!.value, hendelseId = hendelseId)
        when (mottakertype) {
            Mottakertype.ARBEIDSGIVER -> utbetalingTilArbeidsgiver(utbetalingtype, person, arbeidsgiver, vedtaksperiode, behandling)
            Mottakertype.SYKMELDT -> utbetalingTilPerson(utbetalingtype, person, arbeidsgiver, vedtaksperiode, behandling)
            Mottakertype.BEGGE -> utbetalingTilBegge(utbetalingtype, person, arbeidsgiver, vedtaksperiode, behandling)
        }
    }

    private fun utbetalingTilArbeidsgiver(
        utbetalingtype: Utbetalingtype,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
        behandling: Behandling,
    ) = utbetalingsopplegg(
        fødselsnummer = person.id.value,
        beløpTilArbeidsgiver = 4000,
        beløpTilSykmeldt = 0,
        utbetalingtype = utbetalingtype,
        utbetalingId = behandling.utbetalingId!!.value,
        vedtaksperiodeId = vedtaksperiode.id.value,
        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
    )

    private fun utbetalingTilPerson(
        utbetalingtype: Utbetalingtype,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
        behandling: Behandling,
    ) = utbetalingsopplegg(
        fødselsnummer = person.id.value,
        beløpTilArbeidsgiver = 0,
        beløpTilSykmeldt = 4000,
        utbetalingtype = utbetalingtype,
        utbetalingId = behandling.utbetalingId!!.value,
        vedtaksperiodeId = vedtaksperiode.id.value,
        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
    )

    private fun utbetalingTilBegge(
        utbetalingtype: Utbetalingtype,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
        behandling: Behandling,
    ) = utbetalingsopplegg(
        fødselsnummer = person.id.value,
        beløpTilArbeidsgiver = 2000,
        beløpTilSykmeldt = 2000,
        utbetalingtype = utbetalingtype,
        utbetalingId = behandling.utbetalingId!!.value,
        vedtaksperiodeId = vedtaksperiode.id.value,
        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
    )

    private val Arbeidsgiver.organisasjonsnummer get() =
        when (val id = this.id) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
        }
}
