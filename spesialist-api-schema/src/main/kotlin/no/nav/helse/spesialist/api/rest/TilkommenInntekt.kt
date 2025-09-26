package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ApiTilkommenInntektskilde(
    val organisasjonsnummer: String,
    val inntekter: List<ApiTilkommenInntekt>,
)

data class ApiTilkommenInntekt(
    val tilkommenInntektId: UUID,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
    val fjernet: Boolean,
    val erDelAvAktivTotrinnsvurdering: Boolean,
    val events: List<ApiTilkommenInntektEvent>,
)

data class ApiTilkommenInntektInput(
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
)

sealed interface ApiTilkommenInntektEvent {
    @Suppress("ktlint:standard:backing-property-naming")
    val __typename: String
        get() = this::class.java.simpleName.removePrefix("Api")
    val metadata: Metadata

    data class Metadata(
        val sekvensnummer: Int,
        val tidspunkt: LocalDateTime,
        val utfortAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
    )

    data class Endringer(
        val organisasjonsnummer: StringEndring?,
        val periode: DatoPeriodeEndring?,
        val periodebelop: BigDecimalEndring?,
        val ekskluderteUkedager: ListLocalDateEndring?,
    ) {
        data class DatoPeriodeEndring(
            val fra: ApiDatoPeriode,
            val til: ApiDatoPeriode,
        )

        data class BigDecimalEndring(
            val fra: BigDecimal,
            val til: BigDecimal,
        )

        data class StringEndring(
            val fra: String,
            val til: String,
        )

        data class ListLocalDateEndring(
            val fra: List<LocalDate>,
            val til: List<LocalDate>,
        )
    }
}

data class ApiTilkommenInntektOpprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
) : ApiTilkommenInntektEvent

data class ApiTilkommenInntektEndretEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

data class ApiTilkommenInntektFjernetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
) : ApiTilkommenInntektEvent

data class ApiTilkommenInntektGjenopprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

data class LeggTilTilkommenInntektResponse(
    val tilkommenInntektId: UUID,
)
