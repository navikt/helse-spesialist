package no.nav.helse.spesialist.api.graphql.schema

data class Soknad(
    val arbeidGjenopptatt: DateString?,
    val sykmeldingSkrevet: DateTimeString?,
)