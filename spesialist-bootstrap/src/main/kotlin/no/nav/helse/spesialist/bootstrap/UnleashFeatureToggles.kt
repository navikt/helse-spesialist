package no.nav.helse.spesialist.bootstrap

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import no.nav.helse.FeatureToggles

class UnleashFeatureToggles(configuration: Configuration) : FeatureToggles {
    data class Configuration(
        val apiKey: String,
        val apiUrl: String,
        val apiEnv: String,
    )

    private val config: UnleashConfig =
        UnleashConfig.builder()
            .appName("spesialist")
            .instanceId("spesialist")
            .unleashAPI(configuration.apiUrl + "/api")
            .apiKey(configuration.apiKey)
            .environment(configuration.apiEnv)
            .build()

    private val unleash: Unleash = DefaultUnleash(config)

    override fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean {
        return unleash.isEnabled("skal-benytte-ny-totrinnsvurderingslosning")
    }
}
