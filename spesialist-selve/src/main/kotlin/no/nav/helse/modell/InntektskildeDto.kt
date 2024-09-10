package no.nav.helse.modell

import java.time.LocalDate

internal enum class InntektskildetypeDto {
    ORDINÆR,
    ENKELTPERSONFORETAK,
}

internal sealed interface InntektskildeDto {
    val organisasjonsnummer: String
    val type: InntektskildetypeDto
}

internal data class KomplettInntektskildeDto(
    override val organisasjonsnummer: String,
    override val type: InntektskildetypeDto,
    val navn: String,
    val bransjer: List<String>,
    val sistOppdatert: LocalDate,
) : InntektskildeDto

internal data class NyInntektskildeDto(
    override val organisasjonsnummer: String,
    override val type: InntektskildetypeDto,
) : InntektskildeDto
