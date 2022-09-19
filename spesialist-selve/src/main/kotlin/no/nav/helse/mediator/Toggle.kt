package no.nav.helse.mediator

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object GraphQLApi : Toggle("GRAPHQL_ENABLED")
    object GraphQLPlayground : Toggle("GRAPHQL_PLAYGROUND_ENABLED")
    object BeholdForlengelseMedOvergangTilUTS: Toggle("BEHOLD_FORELENGELSER_TIL_UTS")
}
