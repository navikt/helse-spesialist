package no.nav.helse.spesialist.db.testfixtures

import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.db.DataSourceDbQuery
import no.nav.helse.spesialist.db.migrations.FlywayMigrator

open class ModuleIsolatedDBTestFixture(
    moduleLabel: String,
) {
    val database = TestcontainersDatabase(moduleLabel)

    val module = DBModule(database.dbModuleConfiguration)
    private val dbQuery = DataSourceDbQuery(module.dataSource)

    private val flywayMigrator =
        FlywayMigrator(
            jdbcUrl = database.dbModuleConfiguration.jdbcUrl,
            username = database.dbModuleConfiguration.username,
            password = database.dbModuleConfiguration.password,
        )

    init {
        flywayMigrator.migrate()
        truncate()
    }

    fun truncate() {
        dbQuery.execute(
            """
            CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
            DECLARE
            truncate_statement text;
            BEGIN
                SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' RESTART IDENTITY CASCADE'
                    INTO truncate_statement
                FROM pg_tables
                WHERE schemaname='public'
                AND tablename not in ('enhet', 'flyway_schema_history', 'varseldefinisjon');

                EXECUTE truncate_statement;
            END;
            $$ LANGUAGE plpgsql;
            """.trimIndent(),
        )
        dbQuery.execute("SELECT truncate_tables()")
    }
}

object DBTestFixture : ModuleIsolatedDBTestFixture("default")
