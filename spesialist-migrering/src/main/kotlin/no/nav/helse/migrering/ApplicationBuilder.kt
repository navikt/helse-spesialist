package no.nav.helse.migrering

import no.nav.helse.migrering.db.DataSourceBuilder
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(env: Map<String, String>) {
    private val spesialistDataSourceBuilder = DataSourceBuilder(env)
    private val rapidsConnection = RapidApplication.create(env)

    init {
        Personavstemming.River(rapidsConnection, SpesialistDao(spesialistDataSourceBuilder.getDataSource()))
    }

    internal fun start() = rapidsConnection.start()
}
