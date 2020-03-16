package no.nav.helse

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.behov.AvventerGodkjenningBehov
import no.nav.helse.rapids_rivers.RapidApplication

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val dataSourceBuilder = DataSourceBuilder(System.getenv())
    dataSourceBuilder.migrate()
    val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.User)

    RapidApplication.create(System.getenv()).apply {
        AvventerGodkjenningBehov.Factory(this)
    }.start()
}
