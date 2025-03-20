package no.nav.helse.spesialist.client.unleash

import no.nav.helse.FeatureToggles

class ClientUnleashModule(
    configuration: Configuration,
) {
    data class Configuration(
        val apiKey: String,
        val apiUrl: String,
        val apiEnv: String,
    )

    val featureToggles: FeatureToggles =
        UnleashFeatureToggles(
            apiKey = configuration.apiKey,
            apiUrl = configuration.apiUrl,
            apiEnv = configuration.apiEnv,
        )
}
