package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingsgodkjenningCommand(
    private val godkjent: Boolean,
    private val saksbehandlerIdent: String,
    private val oid: UUID,
    private val epostadresse: String,
    private val godkjenttidspunkt: LocalDateTime,
    private val årsak: String?,
    private val begrunnelser: List<String>?,
    private val kommentar: String?,
    private val godkjenningsbehovhendelseId: UUID,
    private val hendelseDao: HendelseDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingsgodkjenningCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val behov = hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId)
        if (godkjent) {
            godkjenningMediator.saksbehandlerUtbetaling(context, behov, vedtaksperiodeId, fødselsnummer, saksbehandlerIdent, epostadresse, godkjenttidspunkt)
        } else {
            godkjenningMediator.saksbehandlerAvvisning(context, behov, vedtaksperiodeId, fødselsnummer, saksbehandlerIdent, epostadresse, godkjenttidspunkt, årsak, begrunnelser, kommentar)
        }
        log.info("sender svar på godkjenningsbehov")
        return true
    }
}

