package no.nav.helse.spesialist.api.modell.saksbehandling.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.modell.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.spesialist.api.modell.Saksbehandler

internal class OverstyrtInntektOgRefusjon(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<OverstyrtArbeidsgiver>,
): Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

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

internal class OverstyrtArbeidsgiver(
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

internal class Refusjonselement(
    private val fom: LocalDate,
    private val tom: LocalDate? = null,
    private val beløp: Double
) {
    internal fun byggEvent() =
        OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiver.OverstyrtRefusjonselement(fom, tom, beløp)
}

