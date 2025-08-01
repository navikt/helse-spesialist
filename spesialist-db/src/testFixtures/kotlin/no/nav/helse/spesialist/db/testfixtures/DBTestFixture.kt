package no.nav.helse.spesialist.db.testfixtures

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.db.migrations.FlywayMigrator
import org.intellij.lang.annotations.Language

open class ModuleIsolatedDBTestFixture(moduleLabel: String) {
    val database = TestcontainersDatabase(moduleLabel)

    val module = DBModule(database.dbModuleConfiguration)
    private val flywayMigrator = FlywayMigrator(
        jdbcUrl = database.dbModuleConfiguration.jdbcUrl,
        username = database.dbModuleConfiguration.username,
        password = database.dbModuleConfiguration.password
    )

    init {
        flywayMigrator.migrate()
        truncate()
    }

    fun truncate() {
        sessionOf(module.dataSource).use {
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
                AND tablename not in ('enhet', 'flyway_schema_history', 'global_snapshot_versjon', 'api_varseldefinisjon');
                UPDATE global_snapshot_versjon SET versjon = 0 WHERE id = 1;

                EXECUTE truncate_statement;
            END;
            $$ LANGUAGE plpgsql;
        """.trimIndent()
            it.run(queryOf(query).asExecute)
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }
}

object DBTestFixture: ModuleIsolatedDBTestFixture("default")
