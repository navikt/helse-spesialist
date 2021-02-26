package no.nav.helse.mediator

abstract class Toggles internal constructor(enabled: Boolean = false, private val force: Boolean = false) {
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

    object FlereRisikobehovEnabled : Toggles(true)
}

internal class MiljøstyrtFeatureToggle(private val env: Map<String, String>) {
    internal fun risikovurdering() = env.getOrDefault("RISK_FEATURE_TOGGLE", "false").toBoolean()
    internal fun automatisering() = env.getOrDefault("AUTOMATISERING_FEATURE_TOGGLE", "false").toBoolean()
    internal fun arbeidsgiverinformasjon() =
        env.getOrDefault("ARBEIDSGIVERINFORMASJON_FEATURE_TOGGLE", "false").toBoolean()
    internal val stikkprøver = env.getOrDefault("STIKKPROEVER_FEATURE_TOGGLE", "false").toBoolean()

    internal fun arbeidsforhold() = env.getOrDefault("ARBEIDSFORHOLD_FEATURE_TOGGLE", "false").toBoolean()
}
