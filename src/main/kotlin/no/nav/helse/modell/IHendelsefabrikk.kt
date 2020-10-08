package no.nav.helse.modell

import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface IHendelsefabrikk {
    fun nyNyVedtaksperiodeEndret(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): NyVedtaksperiodeEndretMessage
    fun nyNyVedtaksperiodeEndret(json: String): NyVedtaksperiodeEndretMessage

    fun nyNyVedtaksperiodeForkastet(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): NyVedtaksperiodeForkastetMessage
    fun nyNyVedtaksperiodeForkastet(json: String): NyVedtaksperiodeForkastetMessage

    fun saksbehandlerløsning(
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        contextId: UUID,
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
    ): SaksbehandlerløsningMessage
    fun saksbehandlerløsning(json: String): SaksbehandlerløsningMessage

    fun nyGodkjenning(
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
    ): NyGodkjenningMessage
    fun nyGodkjenning(json: String): NyGodkjenningMessage

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
    ): OverstyringMessage
    fun overstyring(json: String): OverstyringMessage

    fun tilbakerulling(
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeIder: List<UUID>,
        json: String
    ): NyTilbakerullingMessage
    fun tilbakerulling(json: String): NyTilbakerullingMessage
}
