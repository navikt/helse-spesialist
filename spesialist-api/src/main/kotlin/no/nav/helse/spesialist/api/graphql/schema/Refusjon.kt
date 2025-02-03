package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLRefusjonselement

fun GraphQLArbeidsgiverrefusjon.tilArbeidsgiverrefusjon(): ApiArbeidsgiverrefusjon =
    ApiArbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.tilRefusjonsopplysninger(),
    )

private fun List<GraphQLRefusjonselement>.tilRefusjonsopplysninger() =
    map {
        ApiRefusjonselement(
            fom = it.fom,
            tom = it.tom,
            belop = it.belop,
            meldingsreferanseId = it.meldingsreferanseId,
        )
    }
