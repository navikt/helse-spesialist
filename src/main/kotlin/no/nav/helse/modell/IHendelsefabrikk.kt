package no.nav.helse.modell

import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
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
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype,
        json: String
    ): Godkjenningsbehov
    fun godkjenning(json: String): Godkjenningsbehov

    fun overstyring(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        json: String
    ): Overstyring
    fun overstyring(json: String): Overstyring

    fun tilbakerulling(
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeIder: List<UUID>,
        json: String
    ): Tilbakerulling
    fun tilbakerulling(json: String): Tilbakerulling
    fun utbetalingAnnullert(json: String): UtbetalingAnnullert
}
