package no.nav.helse.modell

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.GosysOppgaveEndret
import no.nav.helse.mediator.meldinger.InnhentSkjermetinfo
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshot
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshotMedWarnings
import no.nav.helse.mediator.meldinger.OverstyringArbeidsforhold
import no.nav.helse.mediator.meldinger.OverstyringInntekt
import no.nav.helse.mediator.meldinger.OverstyringTidslinje
import no.nav.helse.mediator.meldinger.RevurderingAvvist
import no.nav.helse.mediator.meldinger.Saksbehandlerløsning
import no.nav.helse.mediator.meldinger.UtbetalingAnnullert
import no.nav.helse.mediator.meldinger.UtbetalingEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastet
import no.nav.helse.mediator.meldinger.VedtaksperiodeReberegnet
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.overstyring.OverstyringDagDto

internal interface IHendelsefabrikk {
    fun adressebeskyttelseEndret(
        id: UUID,
        fødselsnummer: String,
        json: String
    ): AdressebeskyttelseEndret

    fun adressebeskyttelseEndret(
        json: String
    ): AdressebeskyttelseEndret

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
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        json: String
    ): Godkjenningsbehov
    fun godkjenning(json: String): Godkjenningsbehov

    fun overstyringTidslinje(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        opprettet: LocalDateTime,
        json: String
    ): OverstyringTidslinje

    fun revurderingAvvist(json:String): RevurderingAvvist
    fun revurderingAvvist(fødselsnummer: String, errors: List<String>, json:String): RevurderingAvvist

    fun overstyringTidslinje(json: String): OverstyringTidslinje

    fun overstyringInntekt(json: String): OverstyringInntekt
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
        opprettet: LocalDateTime,
        json: String
    ): OverstyringInntekt

    fun overstyringArbeidsforhold(json: String): OverstyringArbeidsforhold
    fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        organisasjonsnummer: String,
        overstyrteArbeidsforhold : List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String
    ): OverstyringArbeidsforhold

    fun utbetalingAnnullert(json: String): UtbetalingAnnullert
    fun utbetalingEndret(json: String): UtbetalingEndret
    fun oppdaterPersonsnapshot(json: String): OppdaterPersonsnapshot
    fun oppdaterPersonsnapshotMedWarnings(json: String): OppdaterPersonsnapshotMedWarnings
    fun innhentSkjermetinfo(json: String): InnhentSkjermetinfo
    fun vedtaksperiodeReberegnet(json: String): VedtaksperiodeReberegnet
    fun gosysOppgaveEndret(json: String): GosysOppgaveEndret
}
