package no.nav.helse.modell

import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.overstyring.OverstyringDagDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface IHendelsefabrikk {
    fun vedtaksperiodeEndret(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): VedtaksperiodeEndret
    fun vedtaksperiodeEndret(json: String): VedtaksperiodeEndret

    fun vedtaksperiodeForkastet(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): VedtaksperiodeForkastet
    fun vedtaksperiodeForkastet(json: String): VedtaksperiodeForkastet

    fun saksbehandlerløsning(
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        oid: UUID,
        epostadresse: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        oppgaveId: Long,
        json: String
    ): Saksbehandlerløsning
    fun saksbehandlerløsning(json: String): Saksbehandlerløsning

    fun godkjenning(
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        arbeidsforholdId: String?,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
        orgnummereMedAktiveArbeidsforhold: List<String>,
        json: String
    ): Godkjenningsbehov
    fun godkjenning(json: String): Godkjenningsbehov

    fun overstyring(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        json: String
    ): Overstyring
    fun overstyring(json: String): Overstyring


    fun utbetalingAnnullert(json: String): UtbetalingAnnullert
    fun utbetalingEndret(json: String): UtbetalingEndret
    fun oppdaterPersonsnapshot(json: String): OppdaterPersonsnapshot
    fun vedtaksperiodeReberegnet(json: String): VedtaksperiodeReberegnet
}
