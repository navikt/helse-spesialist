package no.nav.helse.mediator

import no.nav.helse.abonnement.GodkjenningsbehovPayload
import no.nav.helse.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.abonnement.OpptegnelseDao
import no.nav.helse.automatiseringsteller
import no.nav.helse.automatiskAvvistÅrsakerTeller
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import java.time.LocalDateTime
import java.util.UUID

internal class GodkjenningMediator(
    private val warningDao: WarningDao,
    private val vedtakDao: VedtakDao,
    private val opptegnelseDao: OpptegnelseDao
) {
    internal fun saksbehandlerUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime
    ) {
        behov.godkjennManuelt(saksbehandlerIdent, saksbehandlerEpost, godkjenttidspunkt)
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeGodkjent(vedtaksperiodeId, fødselsnummer, warningDao, vedtakDao).toJson())
    }

    internal fun saksbehandlerAvvisning(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?
    ) {
        behov.avvisManuelt(saksbehandlerIdent, saksbehandlerEpost, godkjenttidspunkt, årsak, begrunnelser, kommentar)
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, warningDao, vedtakDao).toJson())
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        hendelseId: UUID
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeGodkjent(vedtaksperiodeId, fødselsnummer, warningDao, vedtakDao).toJson())
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, fødselsnummer)
        automatiseringsteller.inc()
    }

    internal fun automatiskAvvisning(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        begrunnelser: List<String>,
        hendelseId: UUID
    ) {
        behov.avvisAutomatisk(begrunnelser)
        context.publiser(behov.toJson())
        context.publiser(behov.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, warningDao, vedtakDao).toJson())
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, fødselsnummer)
        begrunnelser.forEach { automatiskAvvistÅrsakerTeller.labels(it).inc() }
        automatiseringsteller.inc()
    }
}
