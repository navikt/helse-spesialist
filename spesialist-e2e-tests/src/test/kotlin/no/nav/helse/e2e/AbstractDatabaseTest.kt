package no.nav.helse.e2e

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture

abstract class AbstractDatabaseTest {
    protected val environmentToggles: EnvironmentToggles = object : EnvironmentToggles, Map<String, String> by emptyMap() {
        override val ignorerMeldingerForUkjentePersoner = false
        override val kanBeslutteEgneSaker = false
        override val kanGodkjenneUtenBesluttertilgang = false
    }

    protected val dataSource = DBTestFixture.module.dataSource
    protected val daos = DBTestFixture.module.daos
    protected val sessionFactory = DBTestFixture.module.sessionFactory
}
