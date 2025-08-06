package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import java.util.UUID

@GraphQLName("AnnulleringData")
data class ApiAnnulleringData(
    val aktorId: String,
    val fodselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val arsaker: List<ApiAnnulleringArsak>,
    val kommentar: String?,
    val annulleringskandidater: List<UUID>?,
) : HandlingFraApi {
    @GraphQLName("AnnulleringArsak")
    data class ApiAnnulleringArsak(
        val _key: String,
        val arsak: String,
    )
}
