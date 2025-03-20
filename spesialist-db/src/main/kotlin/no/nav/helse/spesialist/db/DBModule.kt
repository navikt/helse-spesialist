package no.nav.helse.spesialist.db

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
