package no.nav.helse.mediator.meldinger.hendelser

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtak.AvsluttetMedVedtak
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetMedVedtakMessage(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtakFattetTidspunkt: LocalDateTime,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val hendelser: List<UUID>,
    private val sykepengegrunnlag: Double,
    private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    private val json: String,
) : Vedtaksperiodemelding {
    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) = person.avsluttetMedVedtak(avsluttetMedVedtak)

    override fun toJson(): String = json

    private val avsluttetMedVedtak get() =
        AvsluttetMedVedtak(
            spleisBehandlingId = spleisBehandlingId,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )
}
