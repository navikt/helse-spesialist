import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.Meldingssender
import no.nav.helse.Meldingssender.sendAktivitetsloggNyAktivitet
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendInntektløsningOld
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendSøknadSendt
import no.nav.helse.Meldingssender.sendUtbetalingEndret
import no.nav.helse.Meldingssender.sendVedtaksperiodeNyUtbetaling
import no.nav.helse.Meldingssender.sendVedtaksperiodeOpprettet
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.oppgaver
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spleis.graphql.HentSnapshot
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao as OpptegnelseApiDao

internal abstract class AbstractE2ETest : AbstractDatabaseTest() {

    protected companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
    }

    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val automatiseringDao = AutomatiseringDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val egenAnsattApiDao = EgenAnsattApiDao(dataSource)

    private val apiVarselRepository = ApiVarselRepository(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    protected val oppgaveDao = OppgaveDao(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    protected val personDao = PersonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    protected val tildelingDao = TildelingDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    private val overstyringApiDao = OverstyringApiDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseApiDao(dataSource)
    protected val saksbehandlerDao = SaksbehandlerDao(dataSource)
    protected val reservasjonDao = ReservasjonDao(dataSource)
    private val notatDao = NotatDao(dataSource)
    private val totrinnsvurderingApiDao = TotrinnsvurderingApiDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    protected val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    protected val snapshotMediator = SnapshotMediator(snapshotApiDao, snapshotClient)

    protected val testRapid = TestRapid()

    protected val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)

    protected val oppgaveMediator =
        OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao, opptegnelseDao)

    private val hendelsefabrikk = Hendelsefabrikk(
        dataSource = dataSource,
        snapshotClient = snapshotClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(vedtakDao, opptegnelseDao),
        automatisering = Automatisering(
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = automatiseringDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            vergemålDao = vergemålDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            overstyringDao = overstyringDao,
            stikkprøver = object : Stikkprøver {
                override fun utsFlereArbeidsgivereFørstegangsbehandling() = false
                override fun utsFlereArbeidsgivereForlengelse() = false
                override fun utsEnArbeidsgiverFørstegangsbehandling() = false
                override fun utsEnArbeidsgiverForlengelse() = false
                override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false
                override fun fullRefusjonFlereArbeidsgivereForlengelse() = false
                override fun fullRefusjonEnArbeidsgiver() = false
            },
            hendelseDao = hendelseDao,
            generasjonDao = generasjonDao,
        ),
        overstyringMediator = OverstyringMediator(testRapid),
        snapshotMediator = snapshotMediator,
        versjonAvKode = "versjonAvKode",
    )
    internal val hendelseMediator = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk
    )

    internal val saksbehandlerMediator = SaksbehandlerMediator(dataSource, testRapid)

    internal val dataFetchingEnvironment = mockk<DataFetchingEnvironment>(relaxed = true)


    internal val personQuery = PersonQuery(
        personApiDao = personApiDao,
        egenAnsattApiDao = egenAnsattApiDao,
        tildelingDao = tildelingDao,
        arbeidsgiverApiDao = arbeidsgiverApiDao,
        overstyringApiDao = overstyringApiDao,
        risikovurderingApiDao = risikovurderingApiDao,
        varselRepository = apiVarselRepository,
        oppgaveApiDao = oppgaveApiDao,
        periodehistorikkDao = periodehistorikkDao,
        notatDao = notatDao,
        totrinnsvurderingApiDao = totrinnsvurderingApiDao,
        snapshotMediator = snapshotMediator,
        reservasjonClient = mockk(relaxed = true),
    )

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
        Meldingssender.testRapid = testRapid
        lagVarseldefinisjoner()
    }

    protected fun settOppBruker(orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList()): UUID {
        val fom = 1.januar
        val tom = 31.januar
        val skjæringstidspunkt = 1.januar
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeOpprettet(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, fom = fom, tom = tom, skjæringstidspunkt = skjæringstidspunkt)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, organisasjonsnummer = ORGNR)
        sendAktivitetsloggNyAktivitet(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, listOf("RV_IM_1"))
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsbehovId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = fom,
            periodeTom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsningComposite(godkjenningsbehovId, ORGNR, VEDTAKSPERIODE_ID)
        if (orgnummereMedRelevanteArbeidsforhold.isNotEmpty()) sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsbehovId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsbehovId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsbehovId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        klargjørForGodkjenning(godkjenningsbehovId)
        return godkjenningsbehovId
    }

    private fun klargjørForGodkjenning(oppgaveId: UUID) {
        sendEgenAnsattløsningOld(oppgaveId, false)
        sendVergemålløsningOld(
            godkjenningsmeldingId = oppgaveId
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = oppgaveId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = oppgaveId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(godkjenningsmeldingId = oppgaveId)
    }

    protected fun person(fødselsnummer: String, aktørId: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(*) FROM person WHERE fodselsnummer = ? AND aktor_id = ?"
            requireNotNull(
                session.run(queryOf(query, fødselsnummer.toLong(), aktørId.toLong()).map { row -> row.int(1) }.asSingle)
            )
        }
    }

    protected fun arbeidsgiver(organisasjonsnummer: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(*) FROM arbeidsgiver WHERE orgnummer = ?"
            requireNotNull(
                session.run(queryOf(query, organisasjonsnummer.toLong()).map { row -> row.int(1) }.asSingle)
            )
        }
    }

    protected fun vedtak(vedtaksperiodeId: UUID): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(*) FROM vedtak WHERE vedtaksperiode_id = ?"
            requireNotNull(
                session.run(queryOf(query, vedtaksperiodeId).map { row -> row.int(1) }.asSingle)
            )
        }
    }

    private fun contextId(hendelseId: UUID): UUID {
        return sessionOf(dataSource).use { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT context_id FROM command_context WHERE hendelse_id = ?",
                        hendelseId
                    ).map { UUID.fromString(it.string("context_id")) }.asSingle
                )
            )
        }
    }

    protected fun assertOppgaver(antall: Int) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(antall, oppgaver.size)
    }

    protected fun assertIngenOppgave() {
        assertEquals(0, testRapid.inspektør.hendelser("oppgave_opprettet").size)
    }

    protected fun vedtaksperiode(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kanAutomatiseres: Boolean = false,
        utbetalingId: UUID,
        snapshot: GraphQLClientResponse<HentSnapshot.Result> = snapshot(utbetalingId = utbetalingId),
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        risikofunn: List<Risikofunn> = emptyList(),
        harOppdatertMetadata: Boolean = false,
    ): UUID {
        every { snapshotClient.hentSnapshot(fødselsnummer) } returns snapshot
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeOpprettet(
            AKTØR,
            FØDSELSNUMMER,
            ORGNR,
            vedtaksperiodeId,
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar
        )
        sendVedtaksperiodeNyUtbetaling(vedtaksperiodeId, utbetalingId, ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, utbetalingId, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            periodetype = Periodetype.FORLENGELSE
        )
        val contextId = contextId(godkjenningsmeldingId)
        sendPersoninfoløsningComposite(
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = godkjenningsmeldingId,
            contextId = contextId
        )
        if (!harOppdatertMetadata) {
            sendArbeidsgiverinformasjonløsningOld(
                hendelseId = godkjenningsmeldingId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                contextId = contextId
            )
        }
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            contextId = contextId
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false,
            contextId = contextId
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            contextId = contextId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId,
            kanGodkjennesAutomatisk = kanAutomatiseres,
            contextId = contextId,
            funn = risikofunn
        )
        return godkjenningsmeldingId
    }

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                    varselkode,
                    "En tittel for varselkode=${varselkode}",
                    "En forklaring for varselkode=${varselkode}",
                    "En handling for varselkode=${varselkode}",
                    false,
                    LocalDateTime.now()
                ).asUpdate)
        }
    }
}
