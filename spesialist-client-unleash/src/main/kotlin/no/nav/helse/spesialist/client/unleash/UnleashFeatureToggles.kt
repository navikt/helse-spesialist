package no.nav.helse.spesialist.client.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import no.nav.helse.FeatureToggles

class UnleashFeatureToggles(
    apiKey: String,
    apiUrl: String,
    apiEnv: String,
) : FeatureToggles {
    private val config: UnleashConfig =
        UnleashConfig.builder()
            .appName("spesialist")
            .instanceId("spesialist")
            .unleashAPI("$apiUrl/api")
            .apiKey(apiKey)
            .environment(apiEnv)
            .build()

    private val unleash: Unleash = DefaultUnleash(config)

    override fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean = unleash.isEnabled("skal-benytte-ny-totrinnsvurderingslosning")
}
