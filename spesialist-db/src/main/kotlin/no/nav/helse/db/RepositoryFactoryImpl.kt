package no.nav.helse.db

import no.nav.helse.db.api.AbonnementDao
import no.nav.helse.db.api.PgAbonnementDao
import javax.sql.DataSource

class RepositoryFactoryImpl(private val dataSource: DataSource) : RepositoryFactory {
    override fun createAbonnementDao(): AbonnementDao = PgAbonnementDao(dataSource)
}
