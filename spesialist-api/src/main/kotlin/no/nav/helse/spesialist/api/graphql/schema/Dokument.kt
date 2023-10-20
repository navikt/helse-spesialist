package no.nav.helse.spesialist.api.graphql.schema

data class Soknad(
    val sendtNav: DateTimeString,
    val soknadsperioder: List<Soknadsperiode>,
    val arbeidGjenopptatt: DateString?,
    val egenmeldingsperioder: List<DatoPeriode>,
    val fravarsperioder: List<Fravarsperiode>
)

data class DatoPeriode(
    val fom: DateString,
    val tom: DateString
)

data class Fravarsperiode(
    val fom: DateString,
    val tom: DateString,
    val fravarstype: Fravarstype,
)

enum class Fravarstype{
    FERIE,
    PERMISJON,
    UTLANDSOPPHOLD,
    UTDANNING_FULLTID,
    UTDANNING_DELTID
}


data class Soknadsperiode(
    val avtaltTimer: Int?,
    val faktiskGrad: Double?,
    val faktiskTimer: Double?,
    val sykemeldingsgrad: Double?,
    val sykemeldingstype: String?,
)