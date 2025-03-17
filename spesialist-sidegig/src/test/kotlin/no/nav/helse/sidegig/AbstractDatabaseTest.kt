package no.nav.helse.sidegig

import no.nav.helse.spesialist.db.testfixtures.DBTestFixture

internal abstract class AbstractDatabaseTest {
    protected val dataSource = DBTestFixture.module.dataSource
    protected val behandlingDao = PgBehandlingDao(dataSource)
}

