package no.nav.helse.modell.oppgave

import no.nav.helse.Oppgavestatus
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

abstract class Command(
    protected val behovId: UUID,
    initiellStatus: Oppgavestatus,
    private val parent: Command?,
    internal val timeout: Duration
) {
    private var status = initiellStatus
    private var ferdigstiltAv: String? = null
    private var oid: UUID? = null
    protected val log: Logger = LoggerFactory.getLogger("command")
    internal open val oppgaver: Set<Command> = setOf()
    internal val oppgavetype: String = requireNotNull(this::class.simpleName)
    internal val invalidert: Boolean = status == Oppgavestatus.Invalidert

    internal abstract fun execute()
    internal open fun fortsett(løsning: HentEnhetLøsning) {
        oppgaver.forEach { it.fortsett(løsning) }
    }

    internal open fun fortsett(løsning: HentPersoninfoLøsning) {
        oppgaver.forEach { it.fortsett(løsning) }
    }

    internal open fun fortsett(løsning: ArbeidsgiverLøsning) {
        oppgaver.forEach { it.fortsett(løsning) }
    }

    internal open fun fortsett(løsning: SaksbehandlerLøsning) {
        oppgaver.forEach { it.fortsett(løsning) }
    }

    protected fun ferdigstill(ident: String, oid: UUID) {
        ferdigstiltAv = ident
        this.oid = oid
        status = Oppgavestatus.Ferdigstilt
    }

    internal open fun invalider() {
        status = Oppgavestatus.Invalidert
        ferdigstiltAv = null
        oid = null
    }

    internal fun ferdigstillSystem() = ferdigstill("Spesialist", SpleisbehovMediator.oid)

    internal fun trengerExecute() = this.ferdigstiltAv == null

    private fun findRootCommand(): Command {
        var current = this
        while (current.parent != null) {
            current = current.parent!!
        }
        return current
    }

    protected fun oppdaterVedtakRef(vedtakRef: Int) {
        (findRootCommand() as Spleisbehov).vedtaksperiodeReferanse = vedtakRef
    }

    fun persisterEndring(oppgaveDao: OppgaveDao) {
        oppgaveDao.updateOppgave(behovId, oppgavetype, status, ferdigstiltAv, oid)
    }

    internal fun persister(oppgaveDao: OppgaveDao, vedtakRef: Int?) {
        val oppgave = oppgaveDao.findOppgave(behovId, oppgavetype)
        if (oppgave != null) {
            log.warn("Prøvde å persistere en oppgave som allerede ligger i databasen")
            return
        }
        require(status != Oppgavestatus.Ferdigstilt) { "Kan ikke persistere en oppgave som er ferdigstilt" }
        oppgaveDao.insertOppgave(behovId, oppgavetype, status, vedtakRef)
    }
}

internal fun List<Command>.execute() = this
    .filter { it.trengerExecute() }
    .forEach { it.execute() }
