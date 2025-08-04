package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OverstyrtArbeidsforhold private constructor(
    id: OverstyringId?,
    override val eksternHendelseId: UUID,
    override val saksbehandlerOid: SaksbehandlerOid,
    override val fødselsnummer: String,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    override val opprettet: LocalDateTime,
    ferdigstilt: Boolean,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
) : Overstyring(id, ferdigstilt) {
    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        legacySaksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_arbeidsforhold"

    companion object {
        fun ny(
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            overstyrteArbeidsforhold: List<Arbeidsforhold>,
        ) = OverstyrtArbeidsforhold(
            id = null,
            eksternHendelseId = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            ferdigstilt = false,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold,
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
            overstyrteArbeidsforhold: List<Arbeidsforhold>,
        ) = OverstyrtArbeidsforhold(
            id = id,
            eksternHendelseId = eksternHendelseId,
            opprettet = opprettet,
            ferdigstilt = ferdigstilt,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold,
        )
    }

    fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): OverstyrtArbeidsforholdEvent =
        OverstyrtArbeidsforholdEvent(
            eksternHendelseId = eksternHendelseId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.map { it.byggEvent() },
        )
}

data class Arbeidsforhold(
    val organisasjonsnummer: String,
    val deaktivert: Boolean,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: Lovhjemmel?,
) {
    fun byggEvent(): OverstyrtArbeidsforholdEvent.Arbeidsforhold =
        OverstyrtArbeidsforholdEvent.Arbeidsforhold(
            orgnummer = organisasjonsnummer,
            deaktivert = deaktivert,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
        )
}
