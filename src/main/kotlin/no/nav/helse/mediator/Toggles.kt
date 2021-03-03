package no.nav.helse.mediator

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.util.UnleashConfig

abstract class Toggles internal constructor(enabled: Boolean = false, private val force: Boolean = false) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    private val states = mutableListOf(enabled)

    val enabled get() = states.last()

    fun enable() {
        if (force) return
        states.add(true)
    }

    fun disable() {
        if (force) return
        states.add(false)
    }

    fun pop() {
        if (states.size == 1) return
        states.removeLast()
    }

    fun enable(block: () -> Unit) {
        enable()
        runWith(block)
    }

    fun disable(block: () -> Unit) {
        disable()
        runWith(block)
    }

    private fun runWith(block: () -> Unit) {
        try {
            block()
        } finally {
            pop()
        }
    }


    object Automatisering : Toggles("AUTOMATISERING_FEATURE_TOGGLE")
    object Arbeidsgiverinformasjon : Toggles("ARBEIDSGIVERINFORMASJON_FEATURE_TOGGLE")
}


object FeatureToggle {

    class Toggle(private val toggleName: String) {
        val enabled get()  = unleash.isEnabled(toggleName, context)
        fun <R> ifEnabled(ifTrue: () -> R, ifFalse: () -> R) = if (enabled) ifTrue() else ifFalse()
        fun enable() = enable(toggleName)
        fun disable() = disable(toggleName)
    }

    private val env = System.getenv("NAIS_CLUSTER_NAME") ?: "test"
    private val fakeUnleash = FakeUnleash()
    private val unleash: Unleash = System.getenv("UNLEASH_URL")?.let {
        DefaultUnleash(
            UnleashConfig.builder()
                .appName(System.getenv("NAIS_APP_NAME"))
                .unleashAPI(it)
                .build()
        )
    } ?: fakeUnleash


    val STIKKPRØVE_TOGGLE = Toggle("tbd.spesialist.stikkprove")
    val ARBEIDSFORHOLD_TOGGLE = Toggle("tbd.spesialist.arbeidsforhold")

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
