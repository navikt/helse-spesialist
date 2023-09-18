package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler

class OverstyrtInntektOgRefusjon(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<OverstyrtArbeidsgiver>,
): Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_inntekt_og_refusjon"

    fun byggEvent(oid: UUID, navn: String, epost: String, ident: String) =
        OverstyrtInntektOgRefusjonEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map(OverstyrtArbeidsgiver::byggEvent),
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost
        )
}

class OverstyrtArbeidsgiver(
    private val organisasjonsnummer: String,
    private val månedligInntekt: Double,
    private val fraMånedligInntekt: Double,
    private val refusjonsopplysninger: List<Refusjonselement>?,
    private val fraRefusjonsopplysninger: List<Refusjonselement>?,
    private val begrunnelse: String,
    private val forklaring: String,
    private val lovhjemmel: Lovhjemmel?,
) {
    fun byggEvent() = OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent(
        organisasjonsnummer,
        månedligInntekt,
        fraMånedligInntekt,
        refusjonsopplysninger = refusjonsopplysninger?.map(Refusjonselement::byggEvent),
        fraRefusjonsopplysninger = fraRefusjonsopplysninger?.map(Refusjonselement::byggEvent),
        begrunnelse = begrunnelse,
        forklaring = forklaring,
        subsumsjon = lovhjemmel?.byggEvent()
    )
}

class Refusjonselement(
    private val fom: LocalDate,
    private val tom: LocalDate? = null,
    private val beløp: Double
) {
    fun byggEvent() =
        OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent.OverstyrtRefusjonselementEvent(fom, tom, beløp)
}

