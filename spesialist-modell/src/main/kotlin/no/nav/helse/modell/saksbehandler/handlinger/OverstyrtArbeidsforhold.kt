package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import java.time.LocalDate
import java.util.UUID

class OverstyrtArbeidsforhold(
    override val id: UUID = UUID.randomUUID(),
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
    val vedtaksperiodeId: UUID,
) : Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer

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
            id = id,
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
    private val lovhjemmel: Lovhjemmel?,
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
