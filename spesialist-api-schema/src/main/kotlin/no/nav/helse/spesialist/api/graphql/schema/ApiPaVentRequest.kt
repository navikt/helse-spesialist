package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("PaVentRequest")
sealed interface ApiPaVentRequest {
    @GraphQLName("FjernPaVentUtenHistorikkinnslag")
    data class ApiFjernPaVentUtenHistorikkinnslag(
        val oppgaveId: Long,
    ) : ApiPaVentRequest
}
