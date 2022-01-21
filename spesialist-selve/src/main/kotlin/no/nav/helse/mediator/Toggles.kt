package no.nav.helse.mediator

abstract class Toggle(internal var enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal fun enable() {
        enabled = true
    }

    internal fun disable() {
        enabled = false
    }

    object GraphQLApi : Toggle("GRAPHQL_ENABLED")
    object GraphQLPlayground : Toggle("GRAPHQL_PLAYGROUND_ENABLED")
}
