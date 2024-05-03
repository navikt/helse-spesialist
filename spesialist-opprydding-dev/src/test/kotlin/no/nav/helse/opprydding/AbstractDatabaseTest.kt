package no.nav.helse.opprydding

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID
import javax.sql.DataSource
import kotlin.random.Random

internal abstract class AbstractDatabaseTest {

    protected val personRepository = PersonRepository(dataSource)

    protected companion object {
        const val FØDSELSNUMMER = "12345678910"

        private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
            withReuse(true)
            withLabel("app-navn", "spesialist-opprydding")
            start()

            println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
        }

        val dataSource =
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = 5
                connectionTimeout = 500
                initializationFailTimeout = 5000
            })

        private fun createTruncateFunction(dataSource: DataSource) {
            sessionOf(dataSource).use {
                @Language("PostgreSQL")
                val query = """
            CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
            DECLARE
            truncate_statement text;
            BEGIN
                SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' RESTART IDENTITY CASCADE'
                    INTO truncate_statement
                FROM pg_tables
                WHERE schemaname='public'
                AND tablename not in ('enhet', 'flyway_schema_history', 'global_snapshot_versjon');
                UPDATE global_snapshot_versjon SET versjon = 0 WHERE id = 1;

                EXECUTE truncate_statement;
            END;
            $$ LANGUAGE plpgsql;
        """
                it.run(queryOf(query).asExecute)
            }
        }

        init {
            Flyway.configure()
                .dataSource(dataSource)
                .placeholders(
                    mapOf("spesialist_oid" to UUID.randomUUID().toString())
                )
                .ignoreMigrationPatterns("*:missing")
                .locations("classpath:db/migration")
                .load()
                .migrate()

            createTruncateFunction(dataSource)
        }
    }

    protected fun opprettPerson(fødselsnummer: String, sequenceNumber: Int = 1) {
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(
                mapOf(
                    "sequence_number" to sequenceNumber.toString(),
                    "saksbehandler_oid" to UUID.randomUUID().toString(),
                    "hendelse_id" to UUID.randomUUID().toString(),
                    "fødselsnummer" to fødselsnummer,
                    "generasjon_id" to UUID.randomUUID().toString(),
                    "vedtaksperiode_id" to UUID.randomUUID().toString(),
                    "command_context_id" to UUID.randomUUID().toString(),
                    "aktør_id" to fødselsnummer.reversed(),
                    "organisasjonsnummer" to Random.nextInt(100000000, 999999999).toString(),
                    "utbetaling_id" to UUID.randomUUID().toString(),
                    "avviksvurdering_unik_id" to UUID.randomUUID().toString(),
                )
            )
            .locations("classpath:db/testperson")
            .load()
            .migrate()
    }

    protected fun assertTabellinnhold(comparison: Comparison, numRows: Int) {
        val tabeller = finnTabeller().toMutableList()
        tabeller.removeAll(
            listOf(
                "flyway_schema_history",
                "flyway_schema_history_backup",
                "enhet",
                "global_snapshot_versjon",
                "saksbehandler",
                "feilende_meldinger",
                "arbeidsgiver",
                "arbeidsgiver_bransjer",
                "arbeidsgiver_navn",
                "api_varseldefinisjon",
                "saksbehandler_opptegnelse_sekvensnummer",
                "inntekt",
                "temp_manglende_varsler",
                "automatisering_korrigert_soknad",
                "arkiv_tildeling_for_oppgaver_uten_utbetaling_id",
                "arkiv_oppgave_uten_utbetaling_id",
                "spesialsak",
                "avviksvurdering_spinnvillgate",
                "vilkarsgrunnlag_per_avviksvurdering_spinnvillgate",
                "sammenligningsgrunnlag_spinnvillgate",
                "passert_filter_for_skjonnsfastsettelse",
            )
        )
        tabeller.forEach { tabellnavn ->
            val expectedRowCount = when (tabellnavn) {
                in listOf("oppdrag", "utbetalingslinje") -> numRows * 2
                in listOf("begrunnelse") -> numRows * 4
                else -> numRows
            }
            val rowCount = finnRowCount(tabellnavn)
            assertTrue(
                comparison.compare(rowCount, expectedRowCount)
            ) { "Table '$tabellnavn' has $rowCount row(s), expected it to be ${comparison.label} $expectedRowCount" }
        }
    }

    protected fun finnTabeller(): List<String> {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            session.run(queryOf(query).map { it.string("table_name") }.asList)
        }
    }

    private fun finnRowCount(tabellnavn: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM $tabellnavn"
            session.run(queryOf(query).map { it.int(1) }.asSingle) ?: 0
        }
    }

    @BeforeEach
    fun resetDatabase() {
        sessionOf(dataSource).use {
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }
}

enum class Comparison(val label: String, val compare: (Int, Int) -> Boolean) {
    EXACTLY("exactly", { a, b -> a == b }),
    AT_LEAST("at least", { a, b -> a >= b })
}
