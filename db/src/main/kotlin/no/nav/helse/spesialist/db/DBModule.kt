package no.nav.helse.spesialist.db

import com.zaxxer.hikari.HikariDataSource
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

    private val _dataSource: HikariDataSource = DataSourceBuilder(configuration).build()
    val dataSource: DataSource = _dataSource
    val daos = DBDaos(dataSource)
    val sessionFactory = TransactionalSessionFactory(dataSource)

    fun shutdown() {
        loggInfo("Forsøker å lukke datasource...")
        _dataSource.close()
        loggInfo("Lukket datasource")
    }
}
