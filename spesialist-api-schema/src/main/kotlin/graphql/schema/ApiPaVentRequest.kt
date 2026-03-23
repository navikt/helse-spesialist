package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.util.UUID

@GraphQLName("PaVentRequest")
sealed interface ApiPaVentRequest {
    @GraphQLName("LeggPaVent")
    data class ApiLeggPaVent(
        val oppgaveId: Long,
        val saksbehandlerOid: UUID,
        val frist: LocalDate,
        val skalTildeles: Boolean,
        val notatTekst: String?,
        val årsaker: List<ApiPaVentArsak>,
    ) : ApiPaVentRequest

    @GraphQLName("FjernPaVent")
    data class ApiFjernPaVent(
        val oppgaveId: Long,
    ) : ApiPaVentRequest

    @GraphQLName("FjernPaVentUtenHistorikkinnslag")
    data class ApiFjernPaVentUtenHistorikkinnslag(
        val oppgaveId: Long,
    ) : ApiPaVentRequest

    @GraphQLName("EndrePaVent")
    data class ApiEndrePaVent(
        val oppgaveId: Long,
        val saksbehandlerOid: UUID,
        val frist: LocalDate,
        val skalTildeles: Boolean,
        val notatTekst: String?,
        val årsaker: List<ApiPaVentArsak>,
    ) : ApiPaVentRequest

    @GraphQLName("PaVentArsak")
    data class ApiPaVentArsak(
        val _key: String,
        val arsak: String,
    )
}
