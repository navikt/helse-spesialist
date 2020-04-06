package no.nav.helse.modell.oppgave

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Oppgavetype
import no.nav.helse.Statustype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.OppgaveDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class OpprettOppgaveCommand(
    private val spleisbehov: Spleisbehov,
    private val oppgaveDao: OppgaveDao
) : Command() {
    private val log = LoggerFactory.getLogger("opprett-oppgave-logger")
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        val oppgaveId = oppgaveDao.insertOppgave(spleisbehov.id, Oppgavetype.GodkjennPeriode, Statustype.Avventer)
        log.info("Opprettet oppgave med {}", keyValue("id", oppgaveId))
    }

}
