package no.nav.helse.e2e

import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.db.bootstrap.DBModule
import no.nav.helse.spesialist.db.testfixtures.TestDatabase

abstract class AbstractDatabaseTest {
    protected val environment: Environment = object : Environment, Map<String, String> by emptyMap() {
        override val brukDummyForKRR = false
        override val ignorerMeldingerForUkjentePersoner = false
        override val kanBeslutteEgneSaker = false
        override val kanGodkjenneUtenBesluttertilgang = false
    }

    companion object {
        protected val dbModule = DBModule(TestDatabase.dbModuleConfiguration)
        init {
            dbModule.flywayMigrator.migrate()
        }
    }
    protected val dataSource = dbModule.dataSource
    protected val daos = dbModule.daos
    protected val sessionFactory = dbModule.sessionFactory
}
