package no.nav.helse.modell

import java.time.LocalDate

enum class InntektskildetypeDto {
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

sealed interface InntektskildeDto {
    val identifikator: String
    val type: InntektskildetypeDto

    private fun gjenopprett(): Inntektskilde {
        val type = type.inntektskildetype()
        return when (this) {
            is NyInntektskildeDto ->
                NyInntektskilde(
                    identifikator = identifikator,
                    type = type,
                )
            is KomplettInntektskildeDto ->
                KomplettInntektskilde(
                    identifikator = identifikator,
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

data class KomplettInntektskildeDto(
    override val identifikator: String,
    override val type: InntektskildetypeDto,
    val navn: String,
    val bransjer: List<String>,
    val sistOppdatert: LocalDate,
) : InntektskildeDto

data class NyInntektskildeDto(
    override val identifikator: String,
    override val type: InntektskildetypeDto,
) : InntektskildeDto
