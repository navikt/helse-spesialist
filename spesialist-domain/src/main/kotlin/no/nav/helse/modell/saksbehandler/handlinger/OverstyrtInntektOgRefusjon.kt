package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OverstyrtInntektOgRefusjon private constructor(
    id: OverstyringId?,
    override val eksternHendelseId: UUID,
    override val saksbehandlerOid: SaksbehandlerOid,
    override val fødselsnummer: String,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    override val opprettet: LocalDateTime,
    ferdigstilt: Boolean,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiver>,
) : Overstyring(id, ferdigstilt) {
    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {
        saksbehandlerWrapper.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_inntekt_og_refusjon"

    companion object {
        fun ny(
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            arbeidsgivere: List<OverstyrtArbeidsgiver>,
        ) = OverstyrtInntektOgRefusjon(
            id = null,
            eksternHendelseId = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            ferdigstilt = false,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere,
        )

        fun fraLagring(
            id: OverstyringId,
            eksternHendelseId: UUID,
            opprettet: LocalDateTime,
            ferdigstilt: Boolean,
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            arbeidsgivere: List<OverstyrtArbeidsgiver>,
        ) = OverstyrtInntektOgRefusjon(
            id = id,
            eksternHendelseId = eksternHendelseId,
            opprettet = opprettet,
            ferdigstilt = ferdigstilt,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere,
        )
    }

    fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ) = OverstyrtInntektOgRefusjonEvent(
        eksternHendelseId = eksternHendelseId,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgivere = arbeidsgivere.map(OverstyrtArbeidsgiver::byggEvent),
        saksbehandlerOid = oid,
        saksbehandlerNavn = navn,
        saksbehandlerIdent = ident,
        saksbehandlerEpost = epost,
    )
}

data class OverstyrtArbeidsgiver(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<Refusjonselement>?,
    val fraRefusjonsopplysninger: List<Refusjonselement>?,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: Lovhjemmel?,
    val fom: LocalDate?,
    val tom: LocalDate?,
) {
    fun byggEvent() =
        OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent(
            organisasjonsnummer,
            månedligInntekt,
            fraMånedligInntekt,
            refusjonsopplysninger = refusjonsopplysninger?.map(Refusjonselement::byggEvent),
            fraRefusjonsopplysninger = fraRefusjonsopplysninger?.map(Refusjonselement::byggEvent),
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            fom = fom,
            tom = tom,
        )
}

data class Refusjonselement(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beløp: Double,
) {
    fun byggEvent() = OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent.OverstyrtRefusjonselementEvent(fom, tom, beløp)
}
