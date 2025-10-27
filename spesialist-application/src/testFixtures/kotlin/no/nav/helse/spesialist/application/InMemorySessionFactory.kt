package no.nav.helse.spesialist.application

import no.nav.helse.db.DokumentDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory

class InMemorySessionFactory(
    notatRepository: InMemoryNotatRepository,
    oppgaveRepository: InMemoryOppgaveRepository,
    notatDao: NotatDao,
    oppgaveDao: OppgaveDao,
    dokumentDao: DokumentDao,
    legacyVedtaksperiodeRepository: InMemoryLegacyVedtaksperiodeRepository,
    dialogDao: InMemoryDialogDao,
    stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
    annulleringRepository: InMemoryAnnulleringRepository,
    saksbehandlerRepository: InMemorySaksbehandlerRepository,
    vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository
) : SessionFactory {
    val sessionContext: InMemorySessionContext = InMemorySessionContext(
        notatRepository,
        oppgaveRepository,
        notatDao,
        oppgaveDao,
        legacyVedtaksperiodeRepository,
        dialogDao,
        stansAutomatiskBehandlingDao,
        annulleringRepository,
        saksbehandlerRepository,
        dokumentDao,
        vedtaksperiodeRepository
    )
    override fun <T> transactionalSessionScope(transactionalBlock: (SessionContext) -> T) =
        transactionalBlock(sessionContext)
}
