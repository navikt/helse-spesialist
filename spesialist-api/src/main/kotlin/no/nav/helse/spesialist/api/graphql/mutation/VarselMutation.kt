package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
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
        // TODO tilgangskontroll
        val antallOppdatert = varselRepository.settStatusVurdert(
            UUID.fromString(generasjonId),
            UUID.fromString(definisjonId),
            varselkode,
            ident,
        )
        return antallOppdatert > 0
    }

    @Suppress("unused")
    fun settStatusAktiv(
        generasjonId: String,
        varselkode: String,
        ident: String,
        env: DataFetchingEnvironment,
    ): Boolean {
        // TODO tilgangskontroll
        val antallOppdatert = varselRepository.settStatusAktiv(
            UUID.fromString(generasjonId),
            varselkode,
            ident,
        )
        return antallOppdatert > 0
    }

}