import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.Meldingssender
import no.nav.helse.Meldingssender.sendArbeidsforholdløsning
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsning
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsning
import no.nav.helse.Meldingssender.sendEgenAnsattløsning
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsning
import no.nav.helse.Meldingssender.sendRisikovurderingløsning
import no.nav.helse.Meldingssender.sendVergemålløsning
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsning
import no.nav.helse.TestRapidHelpers.behov
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.løsning
import no.nav.helse.TestRapidHelpers.løsninger
import no.nav.helse.TestRapidHelpers.oppgaver
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.mediator.meldinger.TestmeldingfabrikkUtenFnr
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.HentSnapshot
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
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
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    protected val egenAnsattDao = EgenAnsattDao(dataSource)
    private val egenAnsattApiDao = EgenAnsattApiDao(dataSource)

    private val varselDao = VarselDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    protected val oppgaveDao = OppgaveDao(dataSource)
    protected val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    protected val personDao = PersonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val warningDao = WarningDao(dataSource)
    protected val tildelingDao = TildelingDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    protected val overstyringApiDao = OverstyringApiDao(dataSource)
    protected val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val utbetalingDao = UtbetalingDao(dataSource)
    protected val opptegnelseDao = OpptegnelseApiDao(dataSource)
    protected val opptegnelseApiDao = OpptegnelseApiDao(dataSource)
    protected val abonnementDao = AbonnementDao(dataSource)
    protected val saksbehandlerDao = SaksbehandlerDao(dataSource)
    protected val reservasjonDao = ReservasjonDao(dataSource)
    private val notatDao = NotatDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)
    protected val overstyringDao = OverstyringDao(dataSource)
    protected val generasjonDao = GenerasjonDao(dataSource)
    protected val nyVarselDao = no.nav.helse.modell.varsel.VarselDao(dataSource)

    protected val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    protected val snapshotMediator = SnapshotMediator(snapshotApiDao, snapshotClient)

    protected val testRapid = TestRapid()

    protected val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)
    protected val meldingsfabrikkUtenFnr get() = TestmeldingfabrikkUtenFnr()

    protected val oppgaveMediator =
        OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao, opptegnelseDao)
    protected val hendelsefabrikk = Hendelsefabrikk(
        dataSource = dataSource,
        snapshotClient = snapshotClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao, opptegnelseDao),
        overstyringMediator = OverstyringMediator(testRapid),
        automatisering = Automatisering(
            warningDao = warningDao,
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = automatiseringDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            vergemålDao = vergemålDao,
            snapshotMediator = snapshotMediator,
            overstyringDao = overstyringDao,
        ) { false },
        snapshotMediator = snapshotMediator
    )
    internal val hendelseMediator = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        opptegnelseDao = opptegnelseDao,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk
    )

    internal val dataFetchingEnvironment = mockk<DataFetchingEnvironment>(relaxed = true)

    internal val saksbehandlerTilganger = mockk<SaksbehandlerTilganger> {
        every { harTilgangTilSkjermedePersoner() } returns true
        every { harTilgangTilBeslutterOppgaver() } returns true
        every { harTilgangTilRiskOppgaver() } returns true
        every { harTilgangTilKode7() } returns true
    }

    internal val personQuery = PersonQuery(
        personApiDao = personApiDao,
        egenAnsattApiDao = egenAnsattApiDao,
        tildelingDao = tildelingDao,
        arbeidsgiverApiDao = arbeidsgiverApiDao,
        overstyringApiDao = overstyringApiDao,
        risikovurderingApiDao = risikovurderingApiDao,
        varselDao = varselDao,
        oppgaveApiDao = oppgaveApiDao,
        periodehistorikkDao = periodehistorikkDao,
        notatDao = notatDao,
        snapshotMediator = snapshotMediator,
        reservasjonClient = mockk(relaxed = true),
    )

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
        Meldingssender.testRapid = testRapid
    }

    protected fun settOppBruker(orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList()): UUID {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        val godkjenningsbehovId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            1.januar,
            31.januar,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsning(godkjenningsbehovId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsbehovId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            ekstraArbeidsgivere = orgnummereMedRelevanteArbeidsforhold.map {
                ArbeidsgiverinformasjonJson(
                    orgnummer = it,
                    navn = "ghost",
                    bransjer = listOf("bransje")
                )
            }
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsbehovId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        klargjørForGodkjenning(godkjenningsbehovId)
        return godkjenningsbehovId
    }

    protected fun klargjørForGodkjenning(oppgaveId: UUID) {
        sendEgenAnsattløsning(oppgaveId, false)
        sendVergemålløsning(
            godkjenningsmeldingId = oppgaveId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = oppgaveId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = oppgaveId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = oppgaveId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
    }

    protected fun sendVedtakFattet(
        fødselsnummer: String,
        vedtaksperiodeId: UUID
    ) {
        Meldingssender.sendVedtakFattet(fødselsnummer, vedtaksperiodeId)
    }

    /**
     * Denne bidrar, på godt og vondt, til en slags integrasjonstesting mellom API og selve, siden den stort sett kalles
     * fra tester som tester selve, men trigger noe oppførsel fra API-siden, som de forskjellige testene asserter på
     * (at status er "AvventerSystem", blant annet).
     */
    protected fun sendSaksbehandlerløsningFraAPI(
        oppgaveId: Long,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandlerOid: UUID,
        godkjent: Boolean,
        begrunnelser: List<String>? = null,
        kommentar: String? = null
    ): UUID {
        hendelseMediator.håndter(
            godkjenningDTO = GodkjenningDTO(
                oppgaveId,
                godkjent,
                saksbehandlerIdent,
                if (godkjent) null else "årsak",
                begrunnelser,
                kommentar
            ),
            epost = saksbehandlerEpost,
            oid = saksbehandlerOid
        )
        assertEquals("AvventerSystem", testRapid.inspektør.siste("oppgave_oppdatert").path("status").asText())
        val løsning = testRapid.inspektør.siste("saksbehandler_løsning")
        testRapid.sendTestMessage(løsning.toString())
        return UUID.fromString(løsning.path("@id").asText())
    }

    protected fun assertHendelse(hendelseId: UUID) {
        assertEquals(1, sessionOf(dataSource).use {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { row -> row.int(1) }.asSingle)
        })
    }

    protected fun assertIkkeHendelse(hendelseId: UUID) {
        assertEquals(0, sessionOf(dataSource).use {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { row -> row.int(1) }.asSingle)
        })
    }

    protected fun assertVedtak(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertIkkeVedtak(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    protected fun assertPersonEksisterer(fnr: String) {
        assertEquals(1, person(fnr))
    }

    protected fun assertPersonEksistererIkke(fnr: String) {
        assertEquals(0, person(fnr))
    }

    protected fun assertArbeidsgiver(orgnr: String) {
        assertEquals(1, arbeidsgiver(orgnr))
    }

    protected fun person(fnr: String): Int {
        return sessionOf(dataSource).use { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT COUNT(*) FROM person WHERE fodselsnummer = ?",
                        fnr.toLong()
                    ).map { row -> row.int(1) }.asSingle
                )
            )
        }
    }

    protected fun arbeidsgiver(orgnr: String): Int {
        return sessionOf(dataSource).use { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT COUNT(*) FROM arbeidsgiver WHERE orgnummer = ?",
                        orgnr.toLong()
                    ).map { row -> row.int(1) }.asSingle
                )
            )
        }
    }

    protected fun vedtak(vedtaksperiodeId: UUID): Int {
        return sessionOf(dataSource).use { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT COUNT(*) FROM vedtak WHERE vedtaksperiode_id = ?",
                        vedtaksperiodeId
                    ).map { row -> row.int(1) }.asSingle
                )
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

    protected fun assertGodkjenningsbehovløsning(
        godkjent: Boolean,
        saksbehandlerIdent: String,
        block: (JsonNode) -> Unit = {}
    ) {
        testRapid.inspektør.løsning("Godkjenning").apply {
            assertTrue(path("godkjent").isBoolean)
            assertEquals(godkjent, path("godkjent").booleanValue())
            assertEquals(saksbehandlerIdent, path("saksbehandlerIdent").textValue())
            assertNotNull(path("godkjenttidspunkt").asLocalDateTime())
            block(this)
        }
    }

    protected fun assertVedtaksperiodeAvvist(
        periodetype: String,
        begrunnelser: List<String>? = null,
        kommentar: String? = null
    ) {
        testRapid.inspektør.hendelser("vedtaksperiode_avvist").first().let {
            assertEquals(periodetype, it.path("periodetype").asText())
            assertEquals(begrunnelser, it.path("begrunnelser")?.map(JsonNode::asText))
            // TODO: BUG: Vi sender faktisk kommentar som "null", ikke null...
            val faktiskKommentar = it.takeIf { it.hasNonNull("kommentar") }?.get("kommentar")?.asText()
            if (kommentar == null) assertEquals("null", faktiskKommentar)
            else assertEquals(kommentar, faktiskKommentar)
        }
    }

    protected fun assertAutomatisertLøsning(godkjent: Boolean = true, block: (JsonNode) -> Unit = {}) {
        assertGodkjenningsbehovløsning(godkjent, "Automatisk behandlet") {
            assertTrue(it.path("automatiskBehandling").booleanValue())
            block(it)
        }
    }

    protected fun assertGodkjenningsbehovIkkeLøst() {
        assertEquals(0, testRapid.inspektør.løsninger().size)
    }

    protected fun assertBehov(vararg behov: String) {
        assertTrue(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    protected fun assertIkkeEtterspurtBehov(behov: String) {
        assertFalse(testRapid.inspektør.behov().any { it == behov })
    }

    protected fun assertTilstand(hendelseId: UUID, vararg tilstand: String) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE hendelse_id = ? ORDER BY id ASC",
                    hendelseId
                ).map { it.string("tilstand") }.asList
            )
        }.also {
            assertEquals(tilstand.toList(), it)
        }
    }

    protected fun assertAdressebeskyttelse(fnr: String, expected: String) {
        val adressebeskyttelse = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT adressebeskyttelse FROM person_info pi JOIN person ON info_ref = pi.id WHERE fodselsnummer = ?",
                    fnr.toLong()
                ).map { it.string("adressebeskyttelse") }.asSingle
            )
        }
        assertEquals(expected, adressebeskyttelse)
    }

    protected fun assertOppgaver(antall: Int) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(antall, oppgaver.size)
    }

    protected fun assertOppgavestatuser(indeks: Int, vararg status: Oppgavestatus) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(status.toList(), oppgaver[indeks]?.statuser)
    }

    protected fun assertOppgavetype(indeks: Int, type: String) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(type, oppgaver[indeks]?.type)
    }

    protected fun assertIngenOppgave() {
        assertEquals(0, testRapid.inspektør.hendelser("oppgave_opprettet").size)
    }

    protected fun assertSnapshot(forventet: GraphQLClientResponse<HentSnapshot.Result>, vedtaksperiodeId: UUID) {
        assertEquals(forventet.data?.person, sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT data FROM snapshot WHERE id = (SELECT snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> objectMapper.readValue<GraphQLPerson>(row.string("data")) }.asSingle
            )
        })
    }

    protected fun assertWarning(forventet: String, vedtaksperiodeId: UUID) {
        assertTrue(sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId) and (inaktiv_fra is null or inaktiv_fra > now())",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("melding") }.asList
            )
        }.contains(forventet))
    }

    protected fun vedtaksperiode(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kanAutomatiseres: Boolean = false,
        snapshot: GraphQLClientResponse<HentSnapshot.Result> = snapshot(),
        utbetalingId: UUID,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        risikofunn: List<Risikofunn> = emptyList()
    ): UUID {
        every { snapshotClient.hentSnapshot(fødselsnummer) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            periodetype = Periodetype.FORLENGELSE,
            fødselsnummer = fødselsnummer
        )
        val contextId = contextId(godkjenningsmeldingId)
        sendPersoninfoløsning(
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = godkjenningsmeldingId,
            contextId = contextId
        )
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            contextId = contextId
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            contextId = contextId
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false,
            contextId = contextId
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true,
            contextId = contextId
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            contextId = contextId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId,
            kanGodkjennesAutomatisk = kanAutomatiseres,
            contextId = contextId,
            funn = risikofunn
        )
        return godkjenningsmeldingId
    }

}
