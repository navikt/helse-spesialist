package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@GraphQLName("TilkommenInntektskilde")
data class ApiTilkommenInntektskilde(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val inntekter: List<ApiTilkommenInntekt>,
)

@GraphQLName("TilkommenInntekt")
data class ApiTilkommenInntekt(
    val fom: LocalDate,
    val tom: LocalDate,
    val periodeBeløp: BigDecimal,
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

    data class Metadata(
        val sekvensnummer: Int,
        val tidspunkt: LocalDateTime,
        val utførtAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
    )

    data class Endringer(
        val organisasjonsnummer: StringEndring?,
        val fom: LocalDateEndring?,
        val tom: LocalDateEndring?,
        val periodebeløp: BigDecimalEndring?,
        val dager: ListLocalDateEndring?,
    ) {
        data class LocalDateEndring(val fra: LocalDate, val til: LocalDate)

        data class BigDecimalEndring(val fra: BigDecimal, val til: BigDecimal)

        data class StringEndring(val fra: String, val til: String)

        data class ListLocalDateEndring(val fra: List<LocalDate>, val til: List<LocalDate>)
    }
}

@GraphQLName("TilkommenInntektOpprettetEvent")
data class ApiTilkommenInntektOpprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val periodebeløp: BigDecimal,
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
