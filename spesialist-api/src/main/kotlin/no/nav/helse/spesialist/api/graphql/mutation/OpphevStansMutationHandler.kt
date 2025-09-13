package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.saksbehandler.handlinger.ApiOpphevStans

class OpphevStansMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : OpphevStansMutationSchema {
    override fun opphevStans(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> {
        saksbehandlerMediator.håndter(
            handlingFraApi = ApiOpphevStans(fødselsnummer = fodselsnummer, begrunnelse = begrunnelse),
            saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
            tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
        )
        return byggRespons(true)
    }
}
