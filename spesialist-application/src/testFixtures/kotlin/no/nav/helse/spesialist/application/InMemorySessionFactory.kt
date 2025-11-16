package no.nav.helse.spesialist.application

import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory

class InMemorySessionFactory(
    annulleringRepository: InMemoryAnnulleringRepository,
    saksbehandlerRepository: InMemorySaksbehandlerRepository,
    dokumentDao: InMemoryDokumentDao,
    oppgaveRepository: InMemoryOppgaveRepository,
    overstyringRepository: InMemoryOverstyringRepository,
    stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
    behandlingRepository: InMemoryBehandlingRepository,
    dialogRepository: InMemoryDialogRepository,
    egenAnsattDao: DelegatingEgenAnsattDao,
    notatRepository: InMemoryNotatRepository,
    opptegnelseDao: InMemoryOpptegnelseDao,
    personRepository: InMemoryPersonRepository,
    påVentRepository: InMemoryPåVentRepository,
    varseldefinisjonRepository: InMemoryVarseldefinisjonRepository,
    vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
) : SessionFactory {
    val sessionContext: InMemorySessionContext = InMemorySessionContext(
        annulleringRepository = annulleringRepository,
        behandlingRepository = behandlingRepository,
        dialogRepository = dialogRepository,
        egenAnsattDao = egenAnsattDao,
        notatRepository = notatRepository,
        opptegnelseDao = opptegnelseDao,
        overstyringRepository = overstyringRepository,
        personRepository = personRepository,
        påVentRepository = påVentRepository,
        saksbehandlerRepository = saksbehandlerRepository,
        varseldefinisjonRepository = varseldefinisjonRepository,
        vedtaksperiodeRepository = vedtaksperiodeRepository,
        dokumentDao = dokumentDao,
        oppgaveRepository = oppgaveRepository,
        stansAutomatiskBehandlingDao = stansAutomatiskBehandlingDao,
    )
    override fun <T> transactionalSessionScope(transactionalBlock: (SessionContext) -> T) =
        transactionalBlock(sessionContext)
}
