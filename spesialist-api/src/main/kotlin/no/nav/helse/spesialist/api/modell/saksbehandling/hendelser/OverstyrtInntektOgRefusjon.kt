package no.nav.helse.spesialist.api.modell.saksbehandling.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.modell.OverstyrtInntektOgRefusjonEvent

class OverstyrtInntektOgRefusjon(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<OverstyrtArbeidsgiver>,
) {
    internal fun byggEvent(oid: UUID, navn: String, epost: String, ident: String) =
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
    private val subsumsjon: Subsumsjon?,
) {
    internal fun byggEvent() = OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiver(
        organisasjonsnummer,
        månedligInntekt,
        fraMånedligInntekt,
        refusjonsopplysninger = refusjonsopplysninger?.map(Refusjonselement::byggEvent),
        fraRefusjonsopplysninger = fraRefusjonsopplysninger?.map(Refusjonselement::byggEvent),
        begrunnelse = begrunnelse,
        forklaring = forklaring,
        subsumsjon = subsumsjon?.byggEvent()
    )
}

class Refusjonselement(
    private val fom: LocalDate,
    private val tom: LocalDate? = null,
    private val beløp: Double
) {
    internal fun byggEvent() =
        OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiver.OverstyrtRefusjonselement(fom, tom, beløp)
}

class Subsumsjon(
    private val paragraf: String,
    private val ledd: String? = null,
    private val bokstav: String? = null,
) {
    internal fun byggEvent() =
        OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiver.Subsumsjon(paragraf, ledd, bokstav)
}