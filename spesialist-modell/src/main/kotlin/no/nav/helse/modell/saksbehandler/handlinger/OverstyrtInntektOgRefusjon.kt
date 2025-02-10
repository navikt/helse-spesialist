package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import java.time.LocalDate
import java.util.UUID

class OverstyrtInntektOgRefusjon(
    override val eksternHendelseId: UUID = UUID.randomUUID(),
    override val saksbehandler: Saksbehandler,
    override val fødselsnummer: String,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiver>,
) : Overstyring {
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_inntekt_og_refusjon"

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

class OverstyrtArbeidsgiver(
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

class Refusjonselement(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beløp: Double,
) {
    fun byggEvent() = OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent.OverstyrtRefusjonselementEvent(fom, tom, beløp)
}
