package no.nav.helse.modell

import java.time.LocalDate

internal enum class InntektskildetypeDto {
    ORDINÆR,
    ENKELTPERSONFORETAK,
    ;

    fun inntektskildetype(): Inntektskildetype {
        return when (this) {
            ORDINÆR -> Inntektskildetype.ORDINÆR
            ENKELTPERSONFORETAK -> Inntektskildetype.ENKELTPERSONFORETAK
        }
    }
}

internal sealed interface InntektskildeDto {
    val organisasjonsnummer: String
    val type: InntektskildetypeDto

    private fun gjenopprett(): Inntektskilde {
        val type = type.inntektskildetype()
        return when (this) {
            is NyInntektskildeDto ->
                NyInntektskilde(
                    organisasjonsnummer = organisasjonsnummer,
                    type = type,
                )
            is KomplettInntektskildeDto ->
                KomplettInntektskilde(
                    organisasjonsnummer = organisasjonsnummer,
                    type = type,
                    navn = navn,
                    bransjer = bransjer,
                    sistOppdatert = sistOppdatert,
                )
        }
    }

    companion object {
        fun List<InntektskildeDto>.gjenopprett() = map { it.gjenopprett() }
    }
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
