package no.nav.helse.spesialist.api.testfixtures.mutation

import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData

fun annullerMutation(annullering: ApiAnnulleringData): String = asGQL(
    """
    mutation Annuller {
        annuller(annullering: {
            organisasjonsnummer: "${annullering.organisasjonsnummer}",
            fodselsnummer: "${annullering.fodselsnummer}",
            aktorId: "${annullering.aktorId}",
            utbetalingId: "${annullering.utbetalingId}",
            arbeidsgiverFagsystemId: "${annullering.arbeidsgiverFagsystemId}",
            personFagsystemId: "${annullering.personFagsystemId}",
            vedtaksperiodeId: "${annullering.vedtaksperiodeId}",
            kommentar: "${annullering.kommentar}",
            arsaker: ${arsaker(annullering)}
        })
    }
"""
)

private fun arsaker(annullering: ApiAnnulleringData): List<String> = annullering.arsaker.map { arsak ->
    asGQL(
        """
       {
           _key: "${arsak._key}",
           arsak: "${arsak.arsak}",
       }
    """
    )
}
