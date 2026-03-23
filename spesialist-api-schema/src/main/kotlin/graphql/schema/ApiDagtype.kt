package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("Dagtype")
enum class ApiDagtype {
    Sykedag,
    SykedagNav,
    Feriedag,
    Egenmeldingsdag,
    Permisjonsdag,
    Arbeidsdag,
    ArbeidIkkeGjenopptattDag,
    Foreldrepengerdag,
    AAPdag,
    Omsorgspengerdag,
    Pleiepengerdag,
    Svangerskapspengerdag,
    Opplaringspengerdag,
    Dagpengerdag,
    MeldingTilNavdag,

    // OBS! Spleis støtter ikke å motta disse dagene. De brukes kun (🤞) til historikkvisning, altså hvilken dag det ble overstyrt _fra_.
    Avvistdag,
    Helg,
}
