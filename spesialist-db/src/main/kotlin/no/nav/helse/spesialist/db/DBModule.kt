package no.nav.helse.spesialist.db

import no.nav.helse.spesialist.application.logg.loggInfo
import javax.sql.DataSource

class DBModule(
    configuration: Configuration,
) {
    data class Configuration(
        val jdbcUrl: String,
        val username: String,
        val password: String,
    )

    val dataSource: DataSource
        field = DataSourceBuilder(configuration).build()

    val daos = DBDaos(dataSource)
    val sessionFactory = TransactionalSessionFactory(dataSource)

    fun shutdown() {
        loggInfo("Forsøker å lukke datasource...")
        dataSource.close()
        loggInfo("Lukket datasource")
    }
}
