package no.nav.helse.mediator

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val OVERSTYR_INNTEKT = "tbd.overstyr_inntekt"
object FeatureToggle {

    class Toggle(private val toggleName: String) {
        val enabled get() = unleash.isEnabled(toggleName, context)
        fun enable() = enable(toggleName)
        fun disable() = disable(toggleName)
    }

    class ByClusterStrategy : Strategy {
        override fun getName() = "byCluster"

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
            val clusterName = System.getenv("NAIS_CLUSTER_NAME") ?: "NO_CLUSTER_NAME"
            return parameters["cluster"]?.split(",")?.any { it.contains(clusterName, ignoreCase = true) } ?: false
        }
    }

    private val env = System.getenv("NAIS_CLUSTER_NAME") ?: "test"
    private val fakeUnleash = FakeUnleash()
    private val unleash: Unleash = System.getenv("UNLEASH_URL")?.let { unleashUrl: String ->
        DefaultUnleash(
            UnleashConfig.builder()
                .appName(System.getenv("NAIS_APP_NAME"))
                .environment(env)
                .instanceId(System.getenv("NAIS_APP_NAME"))
                .unleashAPI(unleashUrl)
                .build(),
            ByClusterStrategy()
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
