package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLRefusjonselement
import java.time.LocalDate
import java.util.UUID

data class Arbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<Refusjonselement>,
)

data class Refusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
    val meldingsreferanseId: UUID,
)

internal fun GraphQLArbeidsgiverrefusjon.tilArbeidsgiverrefusjon(): Arbeidsgiverrefusjon =
    Arbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.tilRefusjonsopplysninger(),
    )

private fun List<GraphQLRefusjonselement>.tilRefusjonsopplysninger() =
    map {
        Refusjonselement(
            fom = it.fom,
            tom = it.tom,
            belop = it.belop,
            meldingsreferanseId = it.meldingsreferanseId,
        )
    }
