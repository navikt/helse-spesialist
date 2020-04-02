package no.nav.helse.modell.oppgave

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Oppgavetype
import no.nav.helse.Statustype
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.OppgaveDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class OpprettOppgaveOppgave(
    private val spleisBehov: SpleisBehov,
    private val oppgaveDao: OppgaveDao
) : Oppgave() {
    private val log = LoggerFactory.getLogger("opprett-oppgave-logger")
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        val oppgaveId = oppgaveDao.opprettOppgave(spleisBehov.id, Oppgavetype.GodkjennPeriode, Statustype.Avventer)
        log.info("Opprettet oppgave med {}", keyValue("id", oppgaveId))
    }

}
