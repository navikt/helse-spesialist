package no.nav.helse.spesialist.api.overstyring

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties
class OverstyrTidslinjeDto(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagDto>
) {
    @JsonIgnoreProperties
    class OverstyrDagDto(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?
    )
}

data class OverstyrTidslinjeKafkaDto(
    val saksbehandlerEpost: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagKafkaDto>
) {
    data class OverstyrDagKafkaDto(
        val dato: LocalDate,
        val type: Type,
        val fraType: Type,
        val grad: Int?,
        val fraGrad: Int?
    ) {
        enum class Type { Sykedag, SykedagNav, Feriedag, Egenmeldingsdag, Permisjonsdag, Arbeidsdag, Avvistdag }
    }
}