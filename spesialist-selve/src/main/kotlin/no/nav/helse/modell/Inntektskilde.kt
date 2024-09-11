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
    private companion object {
        private const val BRANSJE_PRIVATPERSON = "Privatperson"
    }

    fun mottaLøsninger(
        arbeidsgiverinformasjonløsning: Arbeidsgiverinformasjonløsning?,
        personinfoløsninger: HentPersoninfoløsninger?,
    ): Inntektskilde {
        return when (type) {
            Inntektskildetype.ORDINÆR ->
                arbeidsgiverinformasjonløsning
                    ?.relevantLøsning(organisasjonsnummer)
                    ?.oppdaterInntektskilde()
            Inntektskildetype.ENKELTPERSONFORETAK ->
                personinfoløsninger
                    ?.relevantLøsning(organisasjonsnummer)
                    ?.oppdaterInntektskilde()
        } ?: this
    }

    abstract val organisasjonsnummer: String
    abstract val type: Inntektskildetype

    fun måOppdateres() = this is NyInntektskilde || (this is KomplettInntektskilde && this.erUtdatert())

    private fun Arbeidsgiverinformasjonløsning.ArbeidsgiverDto.oppdaterInntektskilde(): Inntektskilde {
        return KomplettInntektskilde(
            organisasjonsnummer = organisasjonsnummer,
            type = type,
            navn = navn,
            bransjer = bransjer,
            sistOppdatert = LocalDate.now(),
        )
    }

    private fun HentPersoninfoløsning.oppdaterInntektskilde(): Inntektskilde {
        return KomplettInntektskilde(
            organisasjonsnummer = ident,
            type = type,
            navn = navn(),
            bransjer = listOf(BRANSJE_PRIVATPERSON),
            sistOppdatert = LocalDate.now(),
        )
    }
}

internal class NyInntektskilde(
    override val organisasjonsnummer: String,
    override val type: Inntektskildetype,
) : Inntektskilde() {
    fun toDto() =
        NyInntektskildeDto(
            organisasjonsnummer = organisasjonsnummer,
            type =
                when (type) {
                    Inntektskildetype.ORDINÆR -> InntektskildetypeDto.ORDINÆR
                    Inntektskildetype.ENKELTPERSONFORETAK -> InntektskildetypeDto.ENKELTPERSONFORETAK
                },
        )
}

internal class KomplettInntektskilde(
    override val organisasjonsnummer: String,
    override val type: Inntektskildetype,
    val navn: String,
    val bransjer: List<String>,
    private val sistOppdatert: LocalDate,
) : Inntektskilde() {
    fun erUtdatert() = sistOppdatert < LocalDate.now().minusDays(1)

    fun toDto() =
        KomplettInntektskildeDto(
            organisasjonsnummer = organisasjonsnummer,
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
