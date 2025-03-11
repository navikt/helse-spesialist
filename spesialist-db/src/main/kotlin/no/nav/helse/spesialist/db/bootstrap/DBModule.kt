package no.nav.helse.spesialist.db.bootstrap

import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.DataSourceBuilder
import no.nav.helse.spesialist.db.FlywayMigrator
import no.nav.helse.spesialist.db.TransactionalSessionFactory

class DBModule(configuration: Configuration) {
    data class Configuration(
        val jdbcUrl: String,
        val username: String,
        val password: String,
    )

    val dataSource = DataSourceBuilder(configuration).build()
    val daos = DBDaos(dataSource)
    val sessionFactory = TransactionalSessionFactory(dataSource)
    val flywayMigrator = FlywayMigrator(configuration)
}
