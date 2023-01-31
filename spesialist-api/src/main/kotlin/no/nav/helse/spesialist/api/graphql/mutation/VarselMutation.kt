package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository

class VarselMutation(private val varselRepository: ApiVarselRepository) : Mutation {

    @Suppress("unused")
    fun settStatusVurdert(
        generasjonId: String,
        definisjonId: String,
        varselkode: String,
        ident: String,
        env: DataFetchingEnvironment,
    ): Boolean {
        val oppdatertVarsel = varselRepository.settStatusVurdert(
            UUID.fromString(generasjonId),
            UUID.fromString(definisjonId),
            varselkode,
            ident,
        )
        return oppdatertVarsel != null
    }

    @Suppress("unused")
    fun settStatusAktiv(
        generasjonId: String,
        varselkode: String,
        ident: String,
        env: DataFetchingEnvironment,
    ): Boolean {
        val antallOppdatert = varselRepository.settStatusAktiv(
            UUID.fromString(generasjonId),
            varselkode,
            ident,
        )
        return antallOppdatert > 0
    }

    @Suppress("unused")
    suspend fun settVarselstatusVurdert(
        generasjonIdString: String,
        definisjonIdString: String,
        varselkode: String,
        ident: String,
    ): DataFetcherResult<VarselDTO> {
        return withContext(Dispatchers.IO) {
            val generasjonId = UUID.fromString(generasjonIdString)
            if (!varselRepository.erAktiv(varselkode, generasjonId))
                return@withContext newResult<VarselDTO?>().error(getNotActiveError(varselkode, generasjonId)).build()
            val oppdatertVarsel = varselRepository.settStatusVurdert(
                generasjonId,
                UUID.fromString(definisjonIdString),
                varselkode,
                ident,
            )

            oppdatertVarsel
                ?.let { newResult<VarselDTO?>().data(it).build() }
                ?: newResult<VarselDTO?>().error(getUpdateError(varselkode, generasjonId)).build()
        }
    }

    private fun getUpdateError(varselkode: String, generasjonId: UUID): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId")
        .extensions(mapOf("code" to 500))
        .build()

    private fun getNotActiveError(varselkode: String, generasjonId: UUID): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Varsel med varselkode=$varselkode, generasjonId=$generasjonId har ikke status AKTIV")
        .extensions(mapOf("code" to 409))
        .build()

}