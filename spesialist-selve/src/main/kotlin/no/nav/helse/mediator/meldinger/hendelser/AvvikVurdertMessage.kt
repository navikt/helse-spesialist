package no.nav.helse.mediator.meldinger.hendelser

import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import no.nav.helse.modell.vilkårsprøving.BeregningsgrunnlagDto
import no.nav.helse.modell.vilkårsprøving.SammenligningsgrunnlagDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvvikVurdertMessage(
    override val id: UUID,
    private val unikId: UUID,
    private val vilkårsgrunnlagId: UUID?,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime,
    private val avviksprosent: Double,
    private val beregningsgrunnlag: BeregningsgrunnlagDto,
    private val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    private val json: String,
) : Vedtaksperiodemelding {
    private val avviksvurdering
        get() =
            AvviksvurderingDto(
                unikId = unikId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                beregningsgrunnlag = beregningsgrunnlag,
            )

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter { håndterAvvikVurdert(avviksvurdering, transactionalSession) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}
