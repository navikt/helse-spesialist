package no.nav.helse.spesialist.api.graphql.schema

data class Soknad(
    val arbeidGjenopptatt: DateString?,
    val sykmeldingSkrevet: DateTimeString?,
    val egenmeldingsdagerFraSykmelding: List<DateString>?,
    val soknadsperioder: List<Soknadsperioder>?
)

data class Soknadsperioder(
    val fom: DateString,
    val tom: DateString,
    val grad: Int,
    val faktiskGrad: Int?,
)