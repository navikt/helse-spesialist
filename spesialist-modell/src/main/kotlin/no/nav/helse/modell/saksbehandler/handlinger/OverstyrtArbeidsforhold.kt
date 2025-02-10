package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import java.time.LocalDate
import java.util.UUID

class OverstyrtArbeidsforhold(
    override val eksternHendelseId: UUID = UUID.randomUUID(),
    override val saksbehandler: Saksbehandler,
    override val fødselsnummer: String,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
) : Overstyring {
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_arbeidsforhold"

    fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): OverstyrtArbeidsforholdEvent {
        return OverstyrtArbeidsforholdEvent(
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
}

class Arbeidsforhold(
    val organisasjonsnummer: String,
    val deaktivert: Boolean,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: Lovhjemmel?,
) {
    fun byggEvent(): OverstyrtArbeidsforholdEvent.Arbeidsforhold {
        return OverstyrtArbeidsforholdEvent.Arbeidsforhold(
            orgnummer = organisasjonsnummer,
            deaktivert = deaktivert,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
        )
    }
}
