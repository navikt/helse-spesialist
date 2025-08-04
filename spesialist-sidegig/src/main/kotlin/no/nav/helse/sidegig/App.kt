package no.nav.helse.sidegig

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication

internal fun main() {
    val env = System.getenv()
    val rapidApp = RapidApplication.create(env)
    val app = App(rapidApp, env)
    app.setupRivers()
    rapidApp.start()
}

private class App(
    private val rapidsConnection: RapidsConnection,
    env: Map<String, String>,
) {
    private val dataSourceBuilder = DataSourceBuilder(env)

    fun setupRivers() {
        BehandlingOpprettetRiver(rapidsConnection, PgBehandlingDao(dataSourceBuilder.getDataSource()))
    }
}
