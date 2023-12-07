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
    private val beregningsgrunnlag: BeregningsgrunnlagDto,

    ) {

    internal fun toDto() = AvviksvurderingDto(
        unikId = unikId,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        avviksprosent = avviksprosent,
        sammenligningsgrunnlag = sammenligningsgrunnlag,
        beregningsgrunnlag = beregningsgrunnlag
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is Avviksvurdering
        && javaClass == other.javaClass
        && unikId == other.unikId
        && fødselsnummer == other.fødselsnummer
        && skjæringstidspunkt == other.skjæringstidspunkt
        && opprettet.withNano(0) == other.opprettet.withNano(0)
        && avviksprosent == other.avviksprosent
        && sammenligningsgrunnlag == other.sammenligningsgrunnlag
        && beregningsgrunnlag == other.beregningsgrunnlag)

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