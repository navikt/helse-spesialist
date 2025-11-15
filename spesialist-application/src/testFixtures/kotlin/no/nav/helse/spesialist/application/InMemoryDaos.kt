package no.nav.helse.spesialist.application

import no.nav.helse.db.Daos

class InMemoryDaos(
    override val abonnementApiDao: InMemoryAbonnementApiDao,
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val saksbehandlerRepository: InMemorySaksbehandlerRepository,
    override val dokumentDao: InMemoryDokumentDao,
    override val egenAnsattDao: InMemoryEgenAnsattDao,
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
    override val arbeidsgiverApiDao = InMemoryArbeidsgiverApiDao()
    override val behandlingApiRepository = InMemoryBehandlingApiRepository()
    override val behandlingsstatistikkDao = InMemoryBehandlingsstatistikkDao()
    override val commandContextDao = InMemoryCommandContextDao()
    override val definisjonDao = InMemoryDefinisjonDao(varseldefinisjonRepository)
    override val dialogDao = InMemoryDialogDao(dialogRepository)
    override val egenAnsattApiDao = InMemoryEgenAnsattApiDao()
    override val legacyBehandlingDao = InMemoryLegacyBehandlingDao()
    override val legacyVarselDao = InMemoryLegacyVarselDao()
    override val meldingDao = InMemoryMeldingDao()
    override val meldingDuplikatkontrollDao = InMemoryMeldingDuplikatkontrollDao()
    override val notatDao = InMemoryNotatDao(oppgaveRepository, notatRepository)
    override val notatApiDao = InMemoryNotatApiDao()
    override val oppgaveDao = InMemoryOppgaveDao(oppgaveRepository, behandlingRepository, vedtaksperiodeRepository)
    override val oppgaveApiDao = InMemoryOppgaveApiDao(oppgaveRepository, vedtaksperiodeRepository)
    override val overstyringApiDao = InMemoryOverstyringApiDao()
    override val periodehistorikkDao = InMemoryPeriodehistorikkDao()
    override val periodehistorikkApiDao = InMemoryPeriodehistorikkApiDao()
    override val personDao = InMemoryPersonDao(personRepository)
    override val personApiDao = InMemoryPersonApiDao()
    override val personinfoDao = InMemoryPersoninfoDao()
    override val poisonPillDao = InMemoryPoisonPillDao()
    override val påVentDao = InMemoryPåVentDao(påVentRepository, oppgaveRepository)
    override val påVentApiDao = InMemoryPåVentApiDao()
    override val reservasjonDao = InMemoryReservasjonDao(saksbehandlerRepository)
    override val risikovurderingApiDao = InMemoryRisikovurderingApiDao()
    override val saksbehandlerDao = InMemorySaksbehandlerDao(saksbehandlerRepository)
    override val stansAutomatiskBehandlingSaksbehandlerDao = InMemoryStansAutomatiskBehandlingSaksbehandlerDao()
    override val tildelingDao = InMemoryTildelingDao()
    override val tildelingApiDao = InMemoryTildelingApiDao()
    override val varselApiRepository = InMemoryVarselApiRepository()
    override val vedtakBegrunnelseDao = InMemoryVedtakBegrunnelseDao()
    override val vedtakDao = InMemoryVedtakDao()
    override val vergemålApiDao = InMemoryVergemålApiDao()
}

