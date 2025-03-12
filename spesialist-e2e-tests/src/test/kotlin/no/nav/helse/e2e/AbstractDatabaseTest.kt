package no.nav.helse.e2e

import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture

abstract class AbstractDatabaseTest {
    protected val environment: Environment = object : Environment, Map<String, String> by emptyMap() {
        override val brukDummyForKRR = false
        override val ignorerMeldingerForUkjentePersoner = false
        override val kanBeslutteEgneSaker = false
        override val kanGodkjenneUtenBesluttertilgang = false
    }

    protected val dataSource = E2eDBTestFixture.fixture.module.dataSource
    protected val daos = E2eDBTestFixture.fixture.module.daos
    protected val sessionFactory = E2eDBTestFixture.fixture.module.sessionFactory
}

object E2eDBTestFixture {
    val fixture = DBTestFixture("e2e-tests")
}
