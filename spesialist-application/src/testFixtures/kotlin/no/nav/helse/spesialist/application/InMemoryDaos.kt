package no.nav.helse.spesialist.application

import no.nav.helse.db.Daos

class InMemoryDaos(
    override val abonnementApiDao: InMemoryAbonnementApiDao,
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val saksbehandlerRepository: InMemorySaksbehandlerRepository,
    override val dokumentDao: InMemoryDokumentDao,
    override val egenAnsattDao: DelegatingEgenAnsattDao,
    override val oppgaveRepository: InMemoryOppgaveRepository,
    override val opptegnelseDao: InMemoryOpptegnelseDao,
    override val stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
    behandlingRepository: InMemoryBehandlingRepository,
    dialogRepository: InMemoryDialogRepository,
    notatRepository: InMemoryNotatRepository,
    personRepository: InMemoryPersonRepository,
    påVentRepository: InMemoryPåVentRepository,
    varseldefinisjonRepository: InMemoryVarseldefinisjonRepository,
    vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
) : Daos {
    override val arbeidsgiverApiDao = UnimplementedArbeidsgiverApiDao()
    override val behandlingApiRepository = UnimplementedBehandlingApiRepository()
    override val behandlingsstatistikkDao = UnimplementedBehandlingsstatistikkDao()
    override val commandContextDao = InMemoryCommandContextDao()
    override val definisjonDao = UnimplementedDefinisjonDao()
    override val dialogDao = DelegatingDialogDao(dialogRepository)
    override val egenAnsattApiDao = DelegatingEgenAnsattApiDao(personRepository)
    override val legacyBehandlingDao = UnimplementedLegacyBehandlingDao()
    override val legacyVarselDao = UnimplementedLegacyVarselDao()
    override val meldingDao = InMemoryMeldingDao()
    override val meldingDuplikatkontrollDao = NoopMeldingDuplikatkontrollDao()
    override val notatDao = DelegatingNotatDao(oppgaveRepository, notatRepository)
    override val notatApiDao = UnimplementedNotatApiDao()
    override val oppgaveDao = DelegatingOppgaveDao(oppgaveRepository, behandlingRepository, vedtaksperiodeRepository)
    override val oppgaveApiDao = DelegatingOppgaveApiDao(oppgaveRepository, vedtaksperiodeRepository)
    override val overstyringApiDao = UnimplementedOverstyringApiDao()
    override val periodehistorikkDao = UnimplementedPeriodehistorikkDao()
    override val periodehistorikkApiDao = UnimplementedPeriodehistorikkApiDao()
    override val personDao = DelegatingPersonDao(personRepository)
    override val personApiDao = UnimplementedPersonApiDao()
    override val personinfoDao = UnimplementedPersoninfoDao()
    override val poisonPillDao = NoopPoisonPillDao()
    override val påVentDao = DelegatingPåVentDao(påVentRepository, oppgaveRepository)
    override val påVentApiDao = UnimplementedPåVentApiDao()
    override val reservasjonDao = InMemoryReservasjonDao(saksbehandlerRepository)
    override val risikovurderingApiDao = UnimplementedRisikovurderingApiDao()
    override val saksbehandlerDao = DelegatingSaksbehandlerDao(saksbehandlerRepository)
    override val stansAutomatiskBehandlingSaksbehandlerDao = UnimplementedStansAutomatiskBehandlingSaksbehandlerDao()
    override val tildelingDao = UnimplementedTildelingDao()
    override val tildelingApiDao = UnimplementedTildelingApiDao()
    override val varselApiRepository = UnimplementedVarselApiRepository()
    override val vedtakBegrunnelseDao = UnimplementedVedtakBegrunnelseDao()
    override val vedtakDao = UnimplementedVedtakDao()
    override val vergemålApiDao = UnimplementedVergemålApiDao()
}

