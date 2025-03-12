package no.nav.helse.sidegig

import no.nav.helse.spesialist.db.testfixtures.DBTestFixture

internal abstract class AbstractDatabaseTest {
    protected val dataSource = SidegigDBTestFixture.fixture.module.dataSource
    protected val behandlingDao = PgBehandlingDao(dataSource)
}

object SidegigDBTestFixture {
    val fixture = DBTestFixture("sidegig")
}

