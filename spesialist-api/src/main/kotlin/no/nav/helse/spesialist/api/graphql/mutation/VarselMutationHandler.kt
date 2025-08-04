package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.conflict
import no.nav.helse.spesialist.api.graphql.internalServerError
import no.nav.helse.spesialist.api.graphql.mapping.toVarselDto
import no.nav.helse.spesialist.api.graphql.notFound
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO
import java.util.UUID

class VarselMutationHandler(
    private val varselRepository: VarselApiRepository,
) : VarselMutationSchema {
    override fun settVarselstatus(
        generasjonIdString: String,
        varselkode: String,
        ident: String,
        definisjonIdString: String?,
    ): DataFetcherResult<ApiVarselDTO?> {
        val generasjonId = UUID.fromString(generasjonIdString)

        return if (definisjonIdString != null) {
            when (varselRepository.erAktiv(varselkode, generasjonId)) {
                false -> return conflict("Varsel med varselkode=$varselkode, generasjonId=$generasjonId har ikke status AKTIV")
                null -> return notFound("Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId fordi varselet ikke finnes")
                true ->
                    varselRepository.settStatusVurdert(
                        generasjonId = generasjonId,
                        definisjonId = UUID.fromString(definisjonIdString),
                        varselkode = varselkode,
                        ident = ident,
                    )
            }
        } else {
            when (varselRepository.erGodkjent(varselkode, generasjonId)) {
                true -> return conflict("Varsel med varselkode=$varselkode, generasjonId=$generasjonId har status GODKJENT")
                null -> return notFound("Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId fordi varselet ikke finnes")
                false ->
                    varselRepository.settStatusAktiv(
                        generasjonId = generasjonId,
                        varselkode = varselkode,
                        ident = ident,
                    )
            }
        }?.toVarselDto()?.let(::byggRespons)
            ?: internalServerError("Kunne ikke oppdatere varsel med varselkode=$varselkode, generasjonId=$generasjonId")
    }
}
