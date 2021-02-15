package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.OppgaveDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*


internal class OppgaveMakstidCommand(
    private val oppgaveId: Long,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveDao: OppgaveDao,
    private val hendelseDao: HendelseDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val oppgaveMediator: OppgaveMediator
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppgaveMakstidCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val erAktivOppgave = oppgaveDao.venterPåSaksbehandler(oppgaveId)
        val oppgaveMakstidOppnådd = oppgaveDao.finnMakstid(oppgaveId)!! < LocalDateTime.now()

        if (!erAktivOppgave) {
            context.publiser(Oppgave.lagMelding(oppgaveId, oppgaveDao).toJson())
        }

        if (erAktivOppgave && oppgaveMakstidOppnådd) {
            val godkjenningsbehovhendelseId = oppgaveDao.finnHendelseId(oppgaveId)
            val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId)) { "Finner ikke oppgave $oppgaveId" }
            val behov = hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId)

            log.info("Oppgave med oppgaveId=$oppgaveId timet ut fordi makstid er oppnådd.")
            godkjenningMediator.makstidOppnådd(context, behov, vedtaksperiodeId, fødselsnummer)
            oppgaveMediator.makstidOppnådd(oppgave)
        }

        return true
    }
}
