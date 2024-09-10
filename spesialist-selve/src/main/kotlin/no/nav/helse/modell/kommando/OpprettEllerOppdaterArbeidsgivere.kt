package no.nav.helse.modell.kommando

import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import java.time.LocalDate

internal enum class Inntektskildetype {
    ORDINÆR,
    ENKELTPERSONFORETAK,
}

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

internal sealed class Inntektskilde {
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
            bransjer = emptyList(),
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

internal interface InntektskilderRepository {
    fun lagre(inntektskilder: List<InntektskildeDto>)
}

internal class OpprettEllerOppdaterArbeidsgivere(
    inntektskilder: List<Inntektskilde>,
    private val inntektskilderRepository: InntektskilderRepository,
) : Command {
    private val inntektskilderSomMåOppdateres = inntektskilder.somMåOppdateres()

    override fun execute(context: CommandContext): Boolean {
        if (inntektskilderSomMåOppdateres.isEmpty()) return true
        sendBehov(context, inntektskilderSomMåOppdateres)
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val (inntektskilderSomFortsattMåOppdateres, inntektskilderSomSkalLagres) =
            inntektskilderSomMåOppdateres
                .supplerMedLøsninger(context)
                .partition { it.måOppdateres() }

        inntektskilderSomSkalLagres.lagreOppdaterteInntektskilder()

        if (inntektskilderSomFortsattMåOppdateres.isEmpty()) return true
        sendBehov(context, inntektskilderSomFortsattMåOppdateres)
        return false
    }

    private fun sendBehov(
        context: CommandContext,
        inntektskilder: List<Inntektskilde>,
    ) {
        inntektskilder
            .lagBehov()
            .forEach { (behovKey, payload) ->
                context.behov(behovKey, payload)
            }
    }

    private fun List<Inntektskilde>.somMåOppdateres() = filter { it.måOppdateres() }

    private fun List<Inntektskilde>.lagreOppdaterteInntektskilder() {
        val inntektskilderSomSkalLagres =
            this
                .filterIsInstance<KomplettInntektskilde>()
                .map { it.toDto() }
        inntektskilderRepository.lagre(inntektskilderSomSkalLagres)
    }

    private fun List<Inntektskilde>.supplerMedLøsninger(context: CommandContext): List<Inntektskilde> {
        val arbeidsgiverinformasjonløsning = context.get<Arbeidsgiverinformasjonløsning>()
        val personinfoløsninger = context.get<HentPersoninfoløsninger>()
        return this.map {
            it.mottaLøsninger(arbeidsgiverinformasjonløsning, personinfoløsninger)
        }
    }

    private fun List<Inntektskilde>.lagBehov(): Map<String, Map<String, List<String>>> {
        return this
            .groupBy(keySelector = { it.type }, valueTransform = { it.organisasjonsnummer })
            .map { (inntektskildetype, inntektskilder) ->
                when (inntektskildetype) {
                    Inntektskildetype.ORDINÆR -> "Arbeidsgiverinformasjon" to mapOf("organisasjonsnummer" to inntektskilder)
                    Inntektskildetype.ENKELTPERSONFORETAK -> "HentPersoninfoV2" to mapOf("ident" to inntektskilder)
                }
            }.toMap()
    }
}
