package no.nav.helse.mediator

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig


object FeatureToggle {

    class Toggle(private val toggleName: String) {
        val enabled get() = unleash.isEnabled(toggleName, context)
        fun <R> ifEnabled(ifTrue: () -> R, ifFalse: () -> R) = if (enabled) ifTrue() else ifFalse()
        fun enable() = enable(toggleName)
        fun disable() = disable(toggleName)
    }

    class ByEnvironmentStrategy() : Strategy {
        override fun getName() = "byEnvironmentParam"

        override fun isEnabled(parameters: MutableMap<String, String>?) =
            parameters?.get("environment")?.split(",")?.map { it.trim() }?.contains(env) == true
    }

    private val env = System.getenv("NAIS_CLUSTER_NAME") ?: "test"
    private val fakeUnleash = FakeUnleash()
    private val unleash: Unleash = System.getenv("UNLEASH_URL")?.let {
        DefaultUnleash(
            UnleashConfig.builder()
                .appName(System.getenv("NAIS_APP_NAME"))
                .unleashAPI(it)
                .build(),
            ByEnvironmentStrategy()
        )
    } ?: fakeUnleash

    private val context = UnleashContext.builder()
        .environment(env)
        .build()

    private fun enable(toggle: String) {
        fakeUnleash.enable(toggle)
    }

    private fun disable(toggle: String) {
        fakeUnleash.disable(toggle)
    }
}
