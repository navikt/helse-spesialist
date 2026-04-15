package no.nav.helse.spesialist.db

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataSourceBuilder = DataSourceBuilder(configuration)
    private val _dataSource: HikariDataSource = dataSourceBuilder.build()
    val dataSource: DataSource = _dataSource
    val daos = DBDaos(dataSource)
    val sessionFactory = TransactionalSessionFactory(dataSource)
    val opptegnelseListener = PgOpptegnelseListener(
        scope = scope,
        dataSource = dataSource,
        connectionProvider = { dataSourceBuilder.listenNotifyConnection },
    )

    fun shutdown() {
        loggInfo("Forsøker å lukke datasource...")
        scope.cancel()
        _dataSource.close()
        loggInfo("Lukket datasource")
    }
}
