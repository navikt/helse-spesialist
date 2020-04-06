package no.nav.helse.modell.oppgave

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Statustype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class SaksbehandlerGodkjenningCommand(
    private val spleisbehov: Spleisbehov,
    private val oppgaveDao: OppgaveDao
) : Command() {
    private val log = LoggerFactory.getLogger("opprett-oppgave-logger")
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        val oppgaveId = oppgaveDao.insertOppgave(spleisbehov.id, "GodkjennPeriode", Statustype.Avventer)
        log.info("Opprettet oppgave med {}", keyValue("id", oppgaveId))
    }

    override fun fortsett(saksbehandlerLøsning: SaksbehandlerLøsning) {
        oppgaveDao.updateOppgaveBesvart(spleisbehov.id)
        ferdigstilt = LocalDateTime.now()
    }

}
