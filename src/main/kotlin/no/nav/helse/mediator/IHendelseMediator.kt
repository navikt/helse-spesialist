package no.nav.helse.mediator

import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
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
        inntektskilde: Inntektskilde,
        aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
        context: MessageContext
    )

    fun overstyring(message: JsonMessage, id: UUID, fødselsnummer: String, context: MessageContext)

    fun utbetalingEndret(
        fødselsnummer: String,
        organisasjonsnummer: String,
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
