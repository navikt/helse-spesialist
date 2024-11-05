package no.nav.helse.modell

import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import java.time.LocalDate

internal enum class Inntektskildetype {
    ORDINÆR,
    ENKELTPERSONFORETAK,
}

internal sealed class Inntektskilde {
    internal companion object {
        private const val BRANSJE_PRIVATPERSON = "Privatperson"
        internal val BEST_ETTER_DATO = LocalDate.now().minusDays(14)
    }

    fun mottaLøsninger(
        arbeidsgiverinformasjonløsning: Arbeidsgiverinformasjonløsning?,
        personinfoløsninger: HentPersoninfoløsninger?,
    ): Inntektskilde {
        return when (type) {
            Inntektskildetype.ORDINÆR ->
                arbeidsgiverinformasjonløsning
                    ?.relevantLøsning(identifikator)
                    ?.oppdaterInntektskilde()
            Inntektskildetype.ENKELTPERSONFORETAK ->
                personinfoløsninger
                    ?.relevantLøsning(identifikator)
                    ?.oppdaterInntektskilde()
        } ?: this
    }

    abstract val identifikator: String
    abstract val type: Inntektskildetype

    fun måOppdateres() = this is NyInntektskilde || (this is KomplettInntektskilde && this.erUtdatert())

    private fun Arbeidsgiverinformasjonløsning.ArbeidsgiverDto.oppdaterInntektskilde(): Inntektskilde {
        return KomplettInntektskilde(
            identifikator = identifikator,
            type = type,
            navn = navn,
            bransjer = bransjer,
            sistOppdatert = LocalDate.now(),
        )
    }

    private fun HentPersoninfoløsning.oppdaterInntektskilde(): Inntektskilde {
        return KomplettInntektskilde(
            identifikator = ident,
            type = type,
            navn = navn(),
            bransjer = listOf(BRANSJE_PRIVATPERSON),
            sistOppdatert = LocalDate.now(),
        )
    }
}

internal class NyInntektskilde(
    override val identifikator: String,
    override val type: Inntektskildetype,
) : Inntektskilde() {
    fun toDto() =
        NyInntektskildeDto(
            identifikator = identifikator,
            type =
                when (type) {
                    Inntektskildetype.ORDINÆR -> InntektskildetypeDto.ORDINÆR
                    Inntektskildetype.ENKELTPERSONFORETAK -> InntektskildetypeDto.ENKELTPERSONFORETAK
                },
        )
}

internal class KomplettInntektskilde(
    override val identifikator: String,
    override val type: Inntektskildetype,
    val navn: String,
    val bransjer: List<String>,
    private val sistOppdatert: LocalDate,
) : Inntektskilde() {
    fun erUtdatert() = sistOppdatert eldreEnn BEST_ETTER_DATO

    private infix fun LocalDate.eldreEnn(other: LocalDate) = this < other

    fun toDto() =
        KomplettInntektskildeDto(
            identifikator = identifikator,
            type =
                when (type) {
                    Inntektskildetype.ORDINÆR -> InntektskildetypeDto.ORDINÆR
                    Inntektskildetype.ENKELTPERSONFORETAK -> InntektskildetypeDto.ENKELTPERSONFORETAK
                },
            navn = navn,
            bransjer = bransjer.map { it },
            sistOppdatert = LocalDate.now(),
        )
}
