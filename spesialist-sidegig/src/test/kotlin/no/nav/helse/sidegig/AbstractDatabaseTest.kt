package no.nav.helse.sidegig

import no.nav.helse.spesialist.db.bootstrap.DBModule
import no.nav.helse.spesialist.db.testfixtures.TestDatabase

internal abstract class AbstractDatabaseTest {
    companion object {
        protected val dbModule = DBModule(TestDatabase.dbModuleConfiguration)
        init {
            dbModule.flywayMigrator.migrate()
        }
    }
    protected val dataSource = dbModule.dataSource
    protected val behandlingDao = PgBehandlingDao(dataSource)
}

