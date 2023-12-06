package no.nav.helse.modell.avviksvurdering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class Avviksvurdering(
    private val unikId: UUID,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime,
    private val avviksprosent: Double,
    private val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    private val beregningsgrunnlag: BeregningsgrunnlagDto

) {

    internal fun toDto() = AvviksvurderingDto(unikId, fødselsnummer, skjæringstidspunkt, opprettet, avviksprosent, sammenligningsgrunnlag, beregningsgrunnlag)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Avviksvurdering

        if (unikId != other.unikId) return false
        if (fødselsnummer != other.fødselsnummer) return false
        if (skjæringstidspunkt != other.skjæringstidspunkt) return false
        if (avviksprosent != other.avviksprosent) return false
        if (sammenligningsgrunnlag != other.sammenligningsgrunnlag) return false
        if (beregningsgrunnlag != other.beregningsgrunnlag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unikId.hashCode()
        result = 31 * result + fødselsnummer.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + avviksprosent.hashCode()
        result = 31 * result + sammenligningsgrunnlag.hashCode()
        result = 31 * result + beregningsgrunnlag.hashCode()
        return result
    }
}