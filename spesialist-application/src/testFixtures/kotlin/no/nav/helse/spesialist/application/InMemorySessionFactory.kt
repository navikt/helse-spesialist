package no.nav.helse.spesialist.application

import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory

class InMemorySessionFactory(
    notatRepository: InMemoryNotatRepository,
    oppgaveRepository: InMemoryOppgaveRepository,
    notatDao: NotatDao,
    oppgaveDao: OppgaveDao,
    vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
    dialogDao: InMemoryDialogDao,
    stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
) : SessionFactory {
    val sessionContext: InMemorySessionContext = InMemorySessionContext(
        notatRepository,
        oppgaveRepository,
        notatDao,
        oppgaveDao,
        vedtaksperiodeRepository,
        dialogDao,
        stansAutomatiskBehandlingDao,
    )
    override fun <T> transactionalSessionScope(transactionalBlock: (SessionContext) -> T) =
        transactionalBlock(sessionContext)
}
