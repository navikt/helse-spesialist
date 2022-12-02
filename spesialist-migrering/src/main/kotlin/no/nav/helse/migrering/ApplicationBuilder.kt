package no.nav.helse.migrering

import no.nav.helse.migrering.DataSourceBuilder.DbUser.SPARSOM
import no.nav.helse.migrering.DataSourceBuilder.DbUser.SPESIALIST
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(env: Map<String, String>) {
    // Sikrer at man ikke får tilgang til db andre steder enn i dev-gcp
    private val sparsomDataSourceBuilder = DataSourceBuilder(env, SPARSOM)
    private val spesialistDataSourceBuilder = DataSourceBuilder(env, SPESIALIST)
    private val rapidsConnection = RapidApplication.create(env)

    init {
        Personavstemming.River(rapidsConnection, SparsomDao(sparsomDataSourceBuilder.getDataSource()), SpesialistDao(spesialistDataSourceBuilder.getDataSource()))
    }

    internal fun start() = rapidsConnection.start()
}