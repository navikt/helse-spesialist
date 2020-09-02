package no.nav.helse.tildeling

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.arbeidsgiver.insertArbeidsgiver
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import no.nav.helse.modell.command.insertOppgave
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.insertInfotrygdutbetalinger
import no.nav.helse.modell.person.insertPerson
import no.nav.helse.modell.person.insertPersoninfo
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import no.nav.helse.modell.vedtak.snapshot.insertSpeilSnapshot
import no.nav.helse.modell.vedtak.upsertVedtak
import no.nav.helse.objectMapper
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TildelingMediatorTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var dataSource: DataSource
    private var vedtakId by Delegates.notNull<Long>()

    @BeforeAll
    fun setup(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        dataSource = setupDataSource()
        vedtakId = opprettVedtak().toLong()
    }

    @AfterAll
    fun tearDown() {
        embeddedPostgres.close()
    }

    @Test
    fun `lagre en tildeling`() {
        val oppgavereferanse = UUID.randomUUID()
        val saksbehandlerReferanse = UUID.randomUUID()

        opprettSaksbehandler(saksbehandlerReferanse)

        TildelingMediator(dataSource).tildelOppgaveTilSaksbehandler(oppgavereferanse, saksbehandlerReferanse)

        assertEquals(saksbehandlerReferanse, TildelingMediator(dataSource).hentSaksbehandlerFor(oppgavereferanse))
    }

    @Disabled
    @Test
    fun `fjerne en tildeling`() {
        val tildelingMediator = TildelingMediator(dataSource)
        val oppgavereferanse = UUID.randomUUID()
        val saksbehandlerReferanse = UUID.randomUUID()

        opprettSaksbehandler(saksbehandlerReferanse)

        tildelingMediator.tildelOppgaveTilSaksbehandler(oppgavereferanse, saksbehandlerReferanse)
        tildelingMediator.fjernTildeling(oppgavereferanse)

        assertNull(tildelingMediator.hentSaksbehandlerFor(oppgavereferanse))
    }

    @Test
    fun `oid til saksbehandler blir lagt til i saksbehandler payload`() {
        val oppgavereferanse = UUID.randomUUID()
        val saksbehandlerReferanse = UUID.randomUUID()

        opprettSaksbehandler(saksbehandlerReferanse)
        opprettSaksbehandlerOppgave(oppgavereferanse)

        val tildelingMediator = TildelingMediator(dataSource)
        tildelingMediator.tildelOppgaveTilSaksbehandler(oppgavereferanse, saksbehandlerReferanse)

        val oppgaver = finnSaksbehandlerOppgaver()
        val oppgave = oppgaver.first { it.oppgavereferanse == oppgavereferanse }

        assertEquals(saksbehandlerReferanse, oppgave.saksbehandlerOid)
    }

    private fun opprettVedtak() = sessionOf(dataSource, returnGeneratedKey = true).use {
        val personinfo = it.insertPersoninfo("Ola", null, "Nordmann", LocalDate.now(), Kjønn.Ukjent)
        val infotrygdUtbetalinger = it.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val person = it.insertPerson(123456789, 987654321, personinfo, 315, infotrygdUtbetalinger)
        val arbeidsgiver = it.insertArbeidsgiver(98765432, "Boomer AS")
        val speilSnapshot = it.insertSpeilSnapshot("{}")
        it.upsertVedtak(
            vedtaksperiodeId = UUID.randomUUID(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            personRef = person!!.toInt(),
            arbeidsgiverRef = arbeidsgiver!!.toInt(),
            speilSnapshotRef = speilSnapshot
        )
    }

    private fun opprettSaksbehandler(saksbehandlerId: UUID) {
        sessionOf(dataSource).use {
            it.persisterSaksbehandler(saksbehandlerId, "Sara Saksbehandler", "sara.saksbehandler@nav.no")
        }
    }

    private fun opprettSaksbehandlerOppgave(oppgavereferanse: UUID) {
        sessionOf(dataSource).use {
            it.insertOppgave(
                oppgavereferanse,
                "TestOppgave",
                Oppgavestatus.AvventerSaksbehandler,
                null,
                null,
                vedtakRef = vedtakId
            )
        }
    }

    private fun finnSaksbehandlerOppgaver() = sessionOf(dataSource).use {
        it.findSaksbehandlerOppgaver()
    }

    private fun setupDataSource(): DataSource {
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

        return HikariDataSource(hikariConfig).also {
            Flyway
                .configure()
                .dataSource(it)
                .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
                .load()
                .also { flyway ->
                    flyway.clean()
                    flyway.migrate()
                }
        }

    }
}
