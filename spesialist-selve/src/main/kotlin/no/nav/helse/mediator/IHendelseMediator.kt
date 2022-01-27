package no.nav.helse.mediator

import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface IHendelseMediator {
    fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: MessageContext
    )

    fun adressebeskyttelseEndret(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        context: MessageContext
    )

    fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: MessageContext
    )

    fun saksbehandlerløsning(
        message: JsonMessage,
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        saksbehandleroid: UUID,
        saksbehandlerepost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        oppgaveId: Long,
        context: MessageContext
    )

    fun løsning(
        hendelseId: UUID,
        contextId: UUID,
        behovId: UUID,
        løsning: Any,
        context: MessageContext
    )

    fun godkjenningsbehov(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        skjæringstidspunkt: LocalDate,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        arbeidsforholdId: String?,
        periodetype: Periodetype,
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
        orgnummereMedAktiveArbeidsforhold: List<String>,
        context: MessageContext
    )

    fun overstyringTidslinje(id: UUID,
                             fødselsnummer: String,
                             oid: UUID,
                             navn: String,
                             ident: String,
                             epost: String,
                             orgnummer: String,
                             begrunnelse: String,
                             overstyrteDager: List<OverstyringDagDto>,
                             json: String,
                             context: MessageContext)

    fun overstyringInntekt(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        forklaring: String,
        månedligInntekt: Double,
        skjæringstidspunkt: LocalDate,
        json: String,
        context: MessageContext
    )

    fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        erAktivt: Boolean,
        begrunnelse: String,
        forklaring: String,
        skjæringstidspunkt: LocalDate,
        json: String,
        context: MessageContext
    )

    fun utbetalingEndret(
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        utbetalingType : Utbetalingtype,
        message: JsonMessage,
        context: MessageContext
    )

    fun utbetalingAnnullert(
        message: JsonMessage,
        context: MessageContext
    )

    fun oppdaterPersonsnapshot(
        message: JsonMessage,
        context: MessageContext
    )

    fun avbrytSaksbehandling(
        message: JsonMessage,
        context: MessageContext
    )

}
