package no.nav.helse.modell.kommando

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
    private val hendelseDao: HendelseDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingsgodkjenningCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val behov = hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId)
        behov.løs(godkjent, saksbehandlerIdent, godkjenttidspunkt, årsak, begrunnelser, kommentar)
        log.info("sender svar på godkjenningsbehov")
        context.publiser(behov.toJson())
        return true
    }
}

