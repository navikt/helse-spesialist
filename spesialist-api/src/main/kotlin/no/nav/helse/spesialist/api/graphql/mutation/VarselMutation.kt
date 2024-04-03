package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class VarselMutation(private val varselRepository: ApiVarselRepository) : Mutation {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun settVarselstatus(
        generasjonIdString: String,
        varselkode: String,
        ident: String,
        definisjonIdString: String? = null,
    ): DataFetcherResult<VarselDTO> =
        withContext(Dispatchers.IO) {
            val generasjonId = UUID.fromString(generasjonIdString)

            if (definisjonIdString != null) {
                when (varselRepository.erAktiv(varselkode, generasjonId)) {
                    false -> varselError(getNotActiveError(varselkode, generasjonId))
                    null -> varselError(getNotFoundError(varselkode, generasjonId))
                    true ->
                        varselRepository.settStatusVurdert(
                            generasjonId = generasjonId,
                            definisjonId = UUID.fromString(definisjonIdString),
                            varselkode = varselkode,
                            ident = ident,
                        ).graphQlResult(varselkode, generasjonId)
                }
            } else {
                when (varselRepository.erGodkjent(varselkode, generasjonId)) {
                    true -> varselError(getIsGodkjentError(varselkode, generasjonId))
                    null -> varselError(getNotFoundError(varselkode, generasjonId))
                    false ->
                        varselRepository.settStatusAktiv(
                            generasjonId = generasjonId,
                            varselkode = varselkode,
                            ident = ident,
                        ).graphQlResult(varselkode, generasjonId)
                }
            }
        }

    private fun varselError(error: GraphQLError): DataFetcherResult<VarselDTO> = newResult<VarselDTO>().error(error).build()

    private fun VarselDTO?.graphQlResult(
        varselkode: String,
        generasjonId: UUID,
    ): DataFetcherResult<VarselDTO> =
        this?.let { newResult<VarselDTO>().data(it).build() }
            ?: varselError(getUpdateError(varselkode, generasjonId))

    private fun getUpdateError(
        varselkode: String,
        generasjonId: UUID,
    ): GraphQLError {
        val message = "Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 500))
            .build()
    }

    private fun getNotFoundError(
        varselkode: String,
        generasjonId: UUID,
    ): GraphQLError {
        val message =
            "Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId fordi varselet ikke finnes"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 404))
            .build()
    }

    private fun getNotActiveError(
        varselkode: String,
        generasjonId: UUID,
    ): GraphQLError {
        val message = "Varsel med varselkode=$varselkode, generasjonId=$generasjonId har ikke status AKTIV"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 409))
            .build()
    }

    private fun getIsGodkjentError(
        varselkode: String,
        generasjonId: UUID,
    ): GraphQLError {
        val message = "Varsel med varselkode=$varselkode, generasjonId=$generasjonId har status GODKJENT"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 409))
            .build()
    }
}
