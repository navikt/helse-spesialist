package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.slf4j.LoggerFactory

class VarselMutation(private val varselRepository: ApiVarselRepository) : Mutation {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun settVarselstatusVurdert(
        generasjonIdString: String,
        definisjonIdString: String,
        varselkode: String,
        ident: String,
    ): DataFetcherResult<VarselDTO?> {
        return withContext(Dispatchers.IO) {
            val generasjonId = UUID.fromString(generasjonIdString)

            val erAktiv = varselRepository.erAktiv(varselkode, generasjonId)
                ?: return@withContext newResult<VarselDTO?>().error(getNotFoundError(varselkode, generasjonId)).build()

            if (!erAktiv)
                return@withContext newResult<VarselDTO?>().error(getNotActiveError(varselkode, generasjonId)).build()

            varselRepository.settStatusVurdert(
                generasjonId,
                UUID.fromString(definisjonIdString),
                varselkode,
                ident,
            )
                ?.let { newResult<VarselDTO?>().data(it).build() }
                ?: newResult<VarselDTO?>().error(getUpdateError(varselkode, generasjonId)).build()
        }
    }

    @Suppress("unused")
    suspend fun settVarselstatusAktiv(
        generasjonIdString: String,
        varselkode: String,
        ident: String,
    ): DataFetcherResult<VarselDTO?> {
        return withContext(Dispatchers.IO) {
            val generasjonId = UUID.fromString(generasjonIdString)

            val erGodkjent = varselRepository.erGodkjent(varselkode, generasjonId)
                ?: return@withContext newResult<VarselDTO?>().error(getNotFoundError(varselkode, generasjonId)).build()

            if (erGodkjent)
                return@withContext newResult<VarselDTO?>().error(getIsGodkjentError(varselkode, generasjonId)).build()

            varselRepository.settStatusAktiv(
                generasjonId,
                varselkode,
                ident,
            )
                ?.let { newResult<VarselDTO?>().data(it).build() }
                ?: newResult<VarselDTO?>().error(getUpdateError(varselkode, generasjonId)).build()
        }
    }

    @Suppress("unused")
    suspend fun settVarselstatus(
        generasjonIdString: String,
        varselkode: String,
        ident: String,
        definisjonIdString: String? = null,
    ): DataFetcherResult<VarselDTO> = withContext(Dispatchers.IO) {
        val generasjonId = UUID.fromString(generasjonIdString)

        if (definisjonIdString != null) {
            when (varselRepository.erAktiv(varselkode, generasjonId)) {
                true -> varselRepository.settStatusVurdert(
                    generasjonId,
                    UUID.fromString(definisjonIdString),
                    varselkode,
                    ident,
                )
                    ?.let { newResult<VarselDTO>().data(it).build() }
                    ?: newResult<VarselDTO>().error(getUpdateError(varselkode, generasjonId)).build()

                false -> return@withContext newResult<VarselDTO>().error(getNotActiveError(varselkode, generasjonId))
                    .build()

                null -> return@withContext newResult<VarselDTO>().error(getNotFoundError(varselkode, generasjonId))
                    .build()
            }
        } else {
            when (varselRepository.erGodkjent(varselkode, generasjonId)) {
                true -> newResult<VarselDTO>().error(getIsGodkjentError(varselkode, generasjonId)).build()
                false -> varselRepository.settStatusAktiv(
                    generasjonId,
                    varselkode,
                    ident,
                )
                    ?.let { newResult<VarselDTO>().data(it).build() }
                    ?: newResult<VarselDTO>().error(getUpdateError(varselkode, generasjonId)).build()

                null -> newResult<VarselDTO>().error(getNotFoundError(varselkode, generasjonId)).build()
            }
        }
    }

    private fun getUpdateError(varselkode: String, generasjonId: UUID): GraphQLError {
        val message = "Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 500))
            .build()
    }

    private fun getNotFoundError(varselkode: String, generasjonId: UUID): GraphQLError {
        val message =
            "Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId fordi varselet ikke finnes"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 404))
            .build()
    }

    private fun getNotActiveError(varselkode: String, generasjonId: UUID): GraphQLError {
        val message = "Varsel med varselkode=$varselkode, generasjonId=$generasjonId har ikke status AKTIV"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 409))
            .build()
    }

    private fun getIsGodkjentError(varselkode: String, generasjonId: UUID): GraphQLError {
        val message = "Varsel med varselkode=$varselkode, generasjonId=$generasjonId har status GODKJENT"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 409))
            .build()
    }

}
