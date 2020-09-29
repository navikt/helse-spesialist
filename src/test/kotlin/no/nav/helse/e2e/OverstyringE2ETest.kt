package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.SpleisMockClient
import no.nav.helse.accessTokenClient
import no.nav.helse.mediator.kafka.FeatureToggle
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.overstyring.Dagtype
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.vedtak.snapshot.ArbeidsgiverFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.vedtaksperiode.VedtaksperiodeMediator
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OverstyringE2ETest {
    private companion object {
        private val SPESIALIST_OID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SNAPSHOTV1 = "{}"
    }

    private val testRapid = TestRapid()
    private val meldingsfabrikk = Testmeldingfabrikk(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR)
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var overstyringDao: OverstyringDao
    private lateinit var hendelseMediator: HendelseMediator
    private lateinit var vedtaksperiodeMediator: VedtaksperiodeMediator
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)

    private fun nyHendelseId() = UUID.randomUUID()

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        FeatureToggle.nyGodkjenningRiver = true
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection
        val hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
        dataSource = HikariDataSource(hikariConfig)

        val spleisMockClient = SpleisMockClient(FØDSELSNUMMER, AKTØR, ORGNR)
        val speilSnapshotRestClient = SpeilSnapshotRestClient(
            spleisMockClient.client,
            accessTokenClient(),
            "spleisClientId"
        )

        hendelseMediator = HendelseMediator(
            rapidsConnection = testRapid,
            speilSnapshotRestClient = speilSnapshotRestClient,
            dataSource = dataSource,
            spesialistOID = SPESIALIST_OID,
            miljøstyrtFeatureToggle = mockk(relaxed = true)
        )
        oppgaveDao = OppgaveDao(dataSource)
        overstyringDao = OverstyringDao(dataSource)
        vedtaksperiodeMediator = VedtaksperiodeMediator(dataSource, oppgaveDao)
    }

    @BeforeEach
    internal fun setupEach() {
        clearMocks(restClient)
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to SPESIALIST_OID.toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }

        testRapid.reset()
    }

    private fun createHikariConfig(jdbcUrl: String) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

    @AfterAll
    fun deactivate() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun `saksbehandler overstyrer sykdomstidslinje`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1
        val hendelseId = sendGodkjenningsbehov(
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        sendPersoninfoløsning(hendelseId)
        assertSaksbehandlerOppgaveOpprettet(hendelseId)

        sendOverstyrteDager(
            listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )

        assertTrue(overstyringDao.finnOverstyring(FØDSELSNUMMER, ORGNR).isNotEmpty())
        assertTrue(oppgaveDao.finnOppgaver().none { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) })

        sendGodkjenningsbehov(
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        val oppgave = oppgaveDao.finnOppgaver().find { it.fødselsnummer == FØDSELSNUMMER }
        assertNotNull(oppgave)
        assertEquals(SAKSBEHANDLER_EPOST, oppgave.saksbehandlerepost)
    }

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        val spleisbehovId = sendGodkjenningsbehov(
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns objectMapper.writeValueAsString(
            PersonFraSpleisDto(
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                arbeidsgivere = listOf(
                    ArbeidsgiverFraSpleisDto(
                        organisasjonsnummer = ORGNR,
                        id = spleisbehovId,
                        vedtaksperioder = listOf(objectMapper.nullNode())
                    )
                )
            )
        )
        sendPersoninfoløsning(spleisbehovId)

        sendOverstyrteDager(
            listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )

        sendGodkjenningsbehov(
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )

        assertTrue(oppgaveDao.finnOppgaver().any { it.fødselsnummer == FØDSELSNUMMER })

        val snapshot = vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER)
        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere.first().overstyringer
        assertEquals(1, overstyringer.size)
        assertEquals(1, overstyringer.first().overstyrteDager.size)
    }

    private fun assertSaksbehandlerOppgaveOpprettet(hendelseId: UUID) {
        val saksbehandlerOppgaver = oppgaveDao.finnOppgaver()
        assertEquals(1, saksbehandlerOppgaver.filter { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) }.size)
        assertTrue(saksbehandlerOppgaver.any { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) })
    }

    private fun sendGodkjenningsbehov(periodeFom: LocalDate, periodeTom: LocalDate) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGodkjenningsbehov(
                id = id,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                organisasjonsnummer = ORGNR,
                periodeFom = periodeFom,
                periodeTom = periodeTom
            )
        )
    }

    private fun sendPersoninfoløsning(hendelseId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagPersoninfoløsning(
                id = id,
                spleisbehovId = hendelseId,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                organisasjonsnummer = ORGNR,
                contextId = testRapid.inspektør.contextId(hendelseId)
            )
        )
    }

    private fun sendOverstyrteDager(dager: List<OverstyringDagDto>) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagOverstyring(
                id = id,
                dager = dager,
                organisasjonsnummer = ORGNR,
                saksbehandlerEpost = SAKSBEHANDLER_EPOST
            )
        )
    }

    private fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    private fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    private fun TestRapid.RapidInspector.løsning(behov: String) =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }
            .last { it.path("@behov").map(JsonNode::asText).contains(behov) }
            .path("@løsning").path(behov)

    private fun TestRapid.RapidInspector.contextId(hendelseId: UUID) =
        hendelser("behov")
            .last { it.hasNonNull("contextId") && it.path("hendelseId").asText() == hendelseId.toString() }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    private fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID) =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asText() == hendelseId.toString() }
            .path("oppgaveId")
            .asLong()
}
