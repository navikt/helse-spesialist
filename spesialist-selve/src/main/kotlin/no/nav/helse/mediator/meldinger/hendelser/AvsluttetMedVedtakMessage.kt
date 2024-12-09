package no.nav.helse.mediator.meldinger.hendelser

import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetMedVedtak
import java.time.LocalDateTime
import java.util.UUID

internal class AvsluttetMedVedtakMessage(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtakFattetTidspunkt: LocalDateTime,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val hendelser: List<UUID>,
    private val sykepengegrunnlag: Double,
    private val grunnlagForSykepengegrunnlag: Double,
    private val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
    private val begrensning: String,
    private val inntekt: Double,
    private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    private val json: String,
) : Vedtaksperiodemelding {
    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) = person.fattVedtak(avsluttetMedVedtak)

    override fun toJson(): String = json

    private val avsluttetMedVedtak get() =
        AvsluttetMedVedtak(
            spleisBehandlingId = spleisBehandlingId,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )
}
