package no.nav.helse.mediator.kafka

internal object FeatureToggle {
    var nyGodkjenningRiver = false
}

internal class MiljøstyrtFeatureToggle(private val env: Map<String, String>) {
    internal fun risikovurdering() = env.getOrDefault("RISK_FEATURE_TOGGLE", "false").toBoolean()
}
