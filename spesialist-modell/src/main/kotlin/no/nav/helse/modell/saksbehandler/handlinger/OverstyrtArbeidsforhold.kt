package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.dto.ArbeidsforholdDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtArbeidsforholdDto

class OverstyrtArbeidsforhold(
    private val id: UUID = UUID.randomUUID(),
    private val fødselsnummer: String,
    private val aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val overstyrteArbeidsforhold: List<Arbeidsforhold>
): Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_arbeidsforhold"

    fun byggEvent(oid: UUID, navn: String, epost: String, ident: String): OverstyrtArbeidsforholdEvent {
        return OverstyrtArbeidsforholdEvent(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.map { it.byggEvent() }
        )
    }

    fun toDto() = OverstyrtArbeidsforholdDto(
        id = id,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        skjæringstidspunkt = skjæringstidspunkt,
        overstyrteArbeidsforhold = overstyrteArbeidsforhold.map(Arbeidsforhold::toDto)
    )
}

class Arbeidsforhold(
    private val organisasjonsnummer: String,
    private val deaktivert: Boolean,
    private val begrunnelse: String,
    private val forklaring: String
) {
    fun byggEvent(): OverstyrtArbeidsforholdEvent.Arbeidsforhold {
        return OverstyrtArbeidsforholdEvent.Arbeidsforhold(
            orgnummer = organisasjonsnummer,
            deaktivert = deaktivert,
            begrunnelse = begrunnelse,
            forklaring = forklaring
        )
    }

    fun toDto() = ArbeidsforholdDto(
        organisasjonsnummer = organisasjonsnummer,
        deaktivert = deaktivert,
        begrunnelse = begrunnelse,
        forklaring = forklaring,
    )
}