package no.nav.helse.spesialist.api.graphql.schema

data class Soknad(
    val sendtNav: DateTimeString,
    val soknadsperioder: List<Soknadsperiode>,
)

data class Soknadsperiode(
    val avtaltTimer: Int?,
    val faktiskGrad: Double?,
    val faktiskTimer: Double?,
    val sykemeldingsgrad: Double?,
    val sykemeldingstype: String?,
)