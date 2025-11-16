package no.nav.helse.spesialist.application

class InMemoryRepositoriesAndDaos() {
    private val annulleringRepository = InMemoryAnnulleringRepository()
    private val behandlingRepository = InMemoryBehandlingRepository()
    private val dialogRepository = InMemoryDialogRepository()
    private val notatRepository = InMemoryNotatRepository()
    private val personRepository = InMemoryPersonRepository()
    private val påVentRepository = InMemoryPåVentRepository()
    private val saksbehandlerRepository = InMemorySaksbehandlerRepository()
    private val varseldefinisjonRepository = InMemoryVarseldefinisjonRepository()
    private val vedtaksperiodeRepository = InMemoryVedtaksperiodeRepository()

    private val abonnementApiDao = InMemoryAbonnementApiDao()
    private val dokumentDao = InMemoryDokumentDao()
    private val egenAnsattDao = DelegatingEgenAnsattDao(personRepository)
    private val oppgaveRepository = InMemoryOppgaveRepository()
    private val opptegnelseDao = InMemoryOpptegnelseDao(personRepository, abonnementApiDao)
    private val overstyringRepository = InMemoryOverstyringRepository()
    private val stansAutomatiskBehandlingDao = InMemoryStansAutomatiskBehandlingDao()

    val daos = InMemoryDaos(
        abonnementApiDao = abonnementApiDao,
        annulleringRepository = annulleringRepository,
        saksbehandlerRepository = saksbehandlerRepository,
        dokumentDao = dokumentDao,
        egenAnsattDao = egenAnsattDao,
        oppgaveRepository = oppgaveRepository,
        stansAutomatiskBehandlingDao = stansAutomatiskBehandlingDao,
        behandlingRepository = behandlingRepository,
        dialogRepository = dialogRepository,
        notatRepository = notatRepository,
        personRepository = personRepository,
        opptegnelseDao = opptegnelseDao,
        påVentRepository = påVentRepository,
        varseldefinisjonRepository = varseldefinisjonRepository,
        vedtaksperiodeRepository = vedtaksperiodeRepository,
    )

    val sessionFactory = InMemorySessionFactory(
        annulleringRepository = annulleringRepository,
        saksbehandlerRepository = saksbehandlerRepository,
        dokumentDao = dokumentDao,
        egenAnsattDao = egenAnsattDao,
        oppgaveRepository = oppgaveRepository,
        opptegnelseDao = opptegnelseDao,
        overstyringRepository = overstyringRepository,
        stansAutomatiskBehandlingDao = stansAutomatiskBehandlingDao,
        behandlingRepository = behandlingRepository,
        dialogRepository = dialogRepository,
        notatRepository = notatRepository,
        personRepository = personRepository,
        påVentRepository = påVentRepository,
        varseldefinisjonRepository = varseldefinisjonRepository,
        vedtaksperiodeRepository = vedtaksperiodeRepository,
    )
}
