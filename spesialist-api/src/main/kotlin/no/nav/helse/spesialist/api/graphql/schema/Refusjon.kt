package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLRefusjonselement

data class Arbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<Refusjonselement>
)

data class Refusjonselement(
    val fom: DateTimeString,
    val tom: DateTimeString?,
    val belop: Double,
    val meldingsreferanseId: UUIDString
)

internal fun GraphQLArbeidsgiverrefusjon.tilArbeidsgiverrefusjon(): Arbeidsgiverrefusjon =
    Arbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.tilRefusjonsopplysninger()
    )

private fun List<GraphQLRefusjonselement>.tilRefusjonsopplysninger() = map {
    Refusjonselement(
        fom = it.fom,
        tom = it.tom,
        belop = it.belop,
        meldingsreferanseId = it.meldingsreferanseId
    )
}
