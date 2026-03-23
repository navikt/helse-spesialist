package no.nav.helse.e2e

import no.nav.helse.spesialist.db.testfixtures.DBTestFixture

abstract class AbstractDatabaseTest {
    protected val dataSource = DBTestFixture.module.dataSource
    protected val daos = DBTestFixture.module.daos
    protected val sessionFactory = DBTestFixture.module.sessionFactory
}
