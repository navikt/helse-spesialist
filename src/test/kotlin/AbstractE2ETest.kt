import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.clearMocks
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.api.GodkjenningDTO
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractE2ETest {
    protected companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val AKTØR = "999999999"

        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

        private val postgresPath = createTempDir()
        private val embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath)
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()

        private val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres").also(::println)
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
        internal val dataSource = HikariDataSource(hikariConfig)
    }

    protected val testRapid = TestRapid()
    protected val meldingsfabrikk = Testmeldingfabrikk(UNG_PERSON_FNR_2018, AKTØR)
    protected val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val featuretoggleMock = MiljøstyrtFeatureToggle(mapOf(
        "RISK_FEATURE_TOGGLE" to "true"
    ))
    protected var miljøstyrtFeatureToggle = mockk<MiljøstyrtFeatureToggle>(relaxed = true)
    protected val hendelseMediator = HendelseMediator(
        rapidsConnection = testRapid,
        speilSnapshotRestClient = restClient,
        dataSource = dataSource,
        miljøstyrtFeatureToggle = featuretoggleMock
        miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
    )

    @BeforeEach
    internal fun resetDatabase() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    @BeforeEach
    internal fun resetTestSetup() {
        clearMocks(restClient)
        testRapid.reset()
    }

    private fun nyHendelseId() = UUID.randomUUID()

    protected fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
    }

    protected fun sendVedtaksperiodeEndret(orgnr: String, vedtaksperiodeId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(id, vedtaksperiodeId, orgnr))
    }

    protected fun sendGodkjenningsbehov(
        orgnr: String,
        vedtaksperiodeId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        warnings: List<String> = emptyList(),
        periodetype: Saksbehandleroppgavetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
    ) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGodkjenningsbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = orgnr,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
                warnings = warnings,
                periodetype = periodetype
    protected fun sendGodkjenningsbehov(orgnr: String, vedtaksperiodeId: UUID, periodeFom: LocalDate = LocalDate.now(), periodeTom: LocalDate = LocalDate.now()) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagGodkjenningsbehov(id, vedtaksperiodeId, orgnr, periodeFom, periodeTom))
    }

    protected fun sendPersoninfoløsning(hendelseId: UUID, orgnr: String, vedtaksperiodeId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagPersoninfoløsning(
                id,
                hendelseId,
                testRapid.inspektør.contextId(),
                vedtaksperiodeId,
                orgnr
            )
        )
    }

    protected fun sendOverstyrteDager(orgnr: String, saksbehandlerEpost: String, dager: List<OverstyringDagDto>) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagOverstyring(
                id = id,
                dager = dager,
                organisasjonsnummer = orgnr,
                saksbehandlerEpost = saksbehandlerEpost
            )
        )
    }

    protected fun sendPersoninfoløsning(hendelseId: UUID, orgnr: String, vedtaksperiodeId: UUID) =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagPersoninfoløsning(
                    id,
                    hendelseId,
                    testRapid.inspektør.contextId(),
                    vedtaksperiodeId,
                    orgnr
                )
            )
        }

    protected fun sendOverstyrteDager(orgnr: String, saksbehandlerEpost: String, dager: List<OverstyringDagDto>) =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyring(
                    id = id,
                    dager = dager,
                    organisasjonsnummer = orgnr,
                    saksbehandlerEpost = saksbehandlerEpost
                )
            )
        }

    protected fun sendRisikovurderingløsning(
        godkjenningsmeldingId: UUID,
        vedtaksperiodeId: UUID,
        begrunnelser: List<String> = emptyList()
    ) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRisikovurderingløsning(
                    id = id,
                    hendelseId = godkjenningsmeldingId,
                    contextId = testRapid.inspektør.contextId(),
                    vedtaksperiodeId = vedtaksperiodeId,
                    begrunnelserSomAleneKreverManuellBehandling = begrunnelser
    protected fun sendRisikovurderingløsning(godkjenningsmeldingId: UUID) {
        nyHendelseId().also {id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRisikovurderingløsning(
                    id,
                    godkjenningsmeldingId,
                    testRapid.inspektør.contextId()
                )
            )
        }

    }

    protected fun sendSaksbehandlerløsning(
        oppgaveId: Long,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandlerOid: UUID,
        godkjent: Boolean
    ) = nyHendelseId().also { id ->
    protected fun sendSaksbehandlerløsning(oppgaveId: Long, saksbehandlerIdent: String, saksbehandlerEpost: String, saksbehandlerOid: UUID, godkjent: Boolean) = nyHendelseId().also { id ->
        hendelseMediator.håndter(
            GodkjenningDTO(
                oppgaveId,
                godkjent,
                saksbehandlerIdent,
                if (godkjent) null else "årsak",
                null,
                null
            ), saksbehandlerEpost, saksbehandlerOid
        )
        testRapid.sendTestMessage(
            testRapid.inspektør.meldinger().last { it.path("@event_name").asText() == "saksbehandler_løsning" }
                .toString()
        )
    }

    protected fun assertHendelse(hendelseId: UUID) {
        assertEquals(1, using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { it.int(1) }.asSingle)
        })
    }

    protected fun assertVedtak(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertIkkeVedtak(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    protected fun vedtak(vedtaksperiodeId: UUID): Int {
        return using(sessionOf(dataSource)) { session ->
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

    protected fun assertGodkjenningsbehovLøsning(godkjent: Boolean, saksbehandlerIdent: String) {
        assertLøsning("Godkjenning") {
            Assertions.assertTrue(it.path("godkjent").isBoolean)
            assertEquals(godkjent, it.path("godkjent").booleanValue())
            assertEquals(saksbehandlerIdent, it.path("saksbehandlerIdent").textValue())
            Assertions.assertNotNull(it.path("godkjenttidspunkt").asLocalDateTime())
        }
    }

    protected fun assertLøsning(behov: String, assertBlock: (JsonNode) -> Unit) {
        testRapid.inspektør.løsning(behov).also(assertBlock)
    }

    protected fun assertBehov(vararg behov: String) {
        Assertions.assertTrue(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    protected fun assertIkkeBehov(vararg behov: String) {
        Assertions.assertFalse(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    protected fun assertTilstand(hendelseId: UUID, vararg tilstand: String) {
        using(sessionOf(dataSource)) { session ->
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

    protected fun assertOppgave(indeks: Int, vararg status: Oppgavestatus) {
        val oppgaver = mutableListOf<Pair<Long, MutableList<JsonNode>>>()
        testRapid.inspektør.hendelser("oppgave_opprettet")
            .forEach { oppgaver.add(it.path("oppgaveId").asLong() to mutableListOf(it)) }
        testRapid.inspektør.hendelser("oppgave_oppdatert")
            .forEach { oppgave ->
                val id = oppgave.path("oppgaveId").asLong()
                oppgaver.firstOrNull { id == it.first }
                    ?.second?.add(oppgave)
            }

        assertEquals(status.toList(), oppgaver[indeks].second.map {
            Oppgavestatus.valueOf(it.path("status").asText())
        })
    }

    protected fun assertSnapshot(forventet: String, vedtaksperiodeId: UUID) {
        assertEquals(forventet, using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT data FROM speil_snapshot WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { it.string("data") }.asSingle
            )
        })
    }

    protected fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    protected fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    protected fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    protected fun TestRapid.RapidInspector.løsning(behov: String) =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }
            .last { it.path("@behov").map(JsonNode::asText).contains(behov) }
            .path("@løsning").path(behov)

    protected fun TestRapid.RapidInspector.contextId() =
        hendelser("behov")
            .last { it.hasNonNull("contextId") }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    protected fun TestRapid.RapidInspector.oppgaveId() =
        hendelser("oppgave_opprettet")
            .last()
            .path("oppgaveId")
            .asLong()

    protected fun TestRapid.RapidInspector.contextId(hendelseId: UUID) =
        hendelser("behov")
            .last { it.hasNonNull("contextId") && it.path("hendelseId").asText() == hendelseId.toString() }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    protected fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID) =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asText() == hendelseId.toString() }
            .path("oppgaveId")
            .asLong()
}
