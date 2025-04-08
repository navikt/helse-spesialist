package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

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
    val dager: Set<LocalDate>,
    val fjernet: Boolean,
    val events: List<ApiTilkommenInntektEvent>,
)

@GraphQLName("TilkommenInntektEvent")
sealed interface ApiTilkommenInntektEvent {
    val metadata: Metadata

    data class Metadata(
        val sekvensnummer: Int,
        val tidspunkt: Instant,
        val utførtAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
    )

    data class Endringer(
        val organisasjonsnummer: Endring<String>?,
        val fom: Endring<LocalDate>?,
        val tom: Endring<LocalDate>?,
        val periodebeløp: Endring<BigDecimal>?,
        val dager: Endring<Set<LocalDate>>?,
    ) {
        data class Endring<T>(val fra: T, val til: T)
    }
}

@GraphQLName("TilkommenInntektOpprettetEvent")
data class ApiTilkommenInntektOpprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val periodebeløp: BigDecimal,
    val dager: Set<LocalDate>,
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
