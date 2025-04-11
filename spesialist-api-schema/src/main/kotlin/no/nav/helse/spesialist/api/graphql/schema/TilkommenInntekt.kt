package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("TilkommenInntektskilde")
data class ApiTilkommenInntektskilde(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val inntekter: List<ApiTilkommenInntekt>,
)

@GraphQLName("TilkommenInntekt")
data class ApiTilkommenInntekt(
    val tilkommenInntektId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val periodebelop: BigDecimal,
    val dager: List<LocalDate>,
    val fjernet: Boolean,
    val events: List<ApiTilkommenInntektEvent>,
)

@GraphQLName("TilkommenInntektRequest")
data class ApiTilkommenInntektRequest(
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val periodebelop: BigDecimal,
    val dager: List<LocalDate>,
)

@GraphQLName("TilkommenInntektEvent")
sealed interface ApiTilkommenInntektEvent {
    val metadata: Metadata

    @GraphQLName("TilkommenInntektEventMetadata")
    data class Metadata(
        val sekvensnummer: Int,
        val tidspunkt: LocalDateTime,
        val utfortAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
    )

    @GraphQLName("TilkommenInntektEventEndringer")
    data class Endringer(
        val organisasjonsnummer: StringEndring?,
        val fom: LocalDateEndring?,
        val tom: LocalDateEndring?,
        val periodebelop: BigDecimalEndring?,
        val dager: ListLocalDateEndring?,
    ) {
        @GraphQLName("TilkommenInntektEventLocalDateEndring")
        data class LocalDateEndring(val fra: LocalDate, val til: LocalDate)

        @GraphQLName("TilkommenInntektEventBigDecimalEndring")
        data class BigDecimalEndring(val fra: BigDecimal, val til: BigDecimal)

        @GraphQLName("TilkommenInntektEventStringEndring")
        data class StringEndring(val fra: String, val til: String)

        @GraphQLName("TilkommenInntektEventListLocalDateEndring")
        data class ListLocalDateEndring(val fra: List<LocalDate>, val til: List<LocalDate>)
    }
}

@GraphQLName("TilkommenInntektOpprettetEvent")
data class ApiTilkommenInntektOpprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val periodebelop: BigDecimal,
    val dager: List<LocalDate>,
) : ApiTilkommenInntektEvent

@GraphQLName("TilkommenInntektEndretEvent")
data class ApiTilkommenInntektEndretEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

@GraphQLName("TilkommenInntektFjernetEvent")
data class ApiTilkommenInntektFjernetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
) : ApiTilkommenInntektEvent

@GraphQLName("TilkommenInntektGjenopprettetEvent")
data class ApiTilkommenInntektGjenopprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent
