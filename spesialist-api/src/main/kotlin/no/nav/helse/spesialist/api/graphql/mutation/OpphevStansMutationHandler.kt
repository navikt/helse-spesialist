package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.ApiOpphevStans

class OpphevStansMutationHandler(private val saksbehandlerMediator: SaksbehandlerMediator) :
    OpphevStansMutationSchema {
    override fun opphevStans(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(ContextValues.SAKSBEHANDLER)
        saksbehandlerMediator.håndter(ApiOpphevStans(fodselsnummer, begrunnelse), saksbehandler)
        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}
