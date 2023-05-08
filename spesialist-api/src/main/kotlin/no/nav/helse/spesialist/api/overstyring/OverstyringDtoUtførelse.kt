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

data class RefusjonselementDto(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beløp: Double
) {
    fun toMap(): Map<String, Any?> = listOfNotNull(
        "fom" to fom,
        "tom" to tom,
        "beløp" to beløp,
    ).toMap()
}

data class SubsumsjonDto(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
) {

    fun toMap(): Map<String, Any> = listOfNotNull(
        "paragraf" to paragraf,
        ledd?.let { "ledd" to ledd },
        bokstav?.let { "bokstav" to bokstav },
    ).toMap()
}

data class OverstyrArbeidsgiverDto(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<RefusjonselementDto>?,
    val fraRefusjonsopplysninger: List<RefusjonselementDto>?,
    val begrunnelse: String,
    val forklaring: String,
    val subsumsjon: SubsumsjonDto?,
) {
    fun toMap(): Map<String, Any?> = listOfNotNull(
        "organisasjonsnummer" to organisasjonsnummer,
        "månedligInntekt" to månedligInntekt,
        "fraMånedligInntekt" to fraMånedligInntekt,
        "refusjonsopplysninger" to refusjonsopplysninger,
        "fraRefusjonsopplysninger" to fraRefusjonsopplysninger,
        "begrunnelse" to begrunnelse,
        "forklaring" to forklaring,
        "subsumsjon" to subsumsjon,
    ).toMap()
}

data class OverstyrInntektOgRefusjonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrArbeidsgiverDto>,
)