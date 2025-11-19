package no.nav.helse.spesialist.application

class InMemoryRepositoriesAndDaos() {
    private val abonnementApiDao = InMemoryAbonnementApiDao()
    private val annulleringRepository = InMemoryAnnulleringRepository()
    private val arbeidsgiverRepository = InMemoryArbeidsgiverRepository()
    private val avviksvurderingRepository = InMemoryAvviksvurderingRepository()
    private val behandlingRepository = InMemoryBehandlingRepository()
    private val commandContextDao = InMemoryCommandContextDao()
    private val dialogRepository = InMemoryDialogRepository()
    private val dokumentDao = InMemoryDokumentDao()
    private val meldingDao = InMemoryMeldingDao()
    private val notatRepository = InMemoryNotatRepository()
    private val oppgaveRepository = InMemoryOppgaveRepository()
    private val overstyringRepository = InMemoryOverstyringRepository()
    private val personRepository = InMemoryPersonRepository()
    private val personPseudoIdDao = InMemoryPersonPseudoIdDao()
    private val påVentRepository = InMemoryPåVentRepository()
    private val saksbehandlerRepository = InMemorySaksbehandlerRepository()
    private val stansAutomatiskBehandlingDao = InMemoryStansAutomatiskBehandlingDao()
    private val tilkommenInntektRepository = InMemoryTilkommenInntektRepository()
    private val totrinnsvurderingRepository = InMemoryTotrinnsvurderingRepository()
    private val varselRepository = InMemoryVarselRepository()
    private val varseldefinisjonRepository = InMemoryVarseldefinisjonRepository()
    private val vedtaksperiodeRepository = InMemoryVedtaksperiodeRepository()
    private val vedtakBegrunnelseRepository = InMemoryVedtakBegrunnelseRepository()

    private val meldingDuplikatkontrollDao = NoopMeldingDuplikatkontrollDao()
    private val poisonPillDao = NoopPoisonPillDao()

    private val opptegnelseDao = InMemoryOpptegnelseDao(personRepository, abonnementApiDao)
    private val reservasjonDao = InMemoryReservasjonDao(saksbehandlerRepository)

    private val dialogDao = DelegatingDialogDao(dialogRepository)
    private val egenAnsattDao = DelegatingEgenAnsattDao(personRepository)
    private val egenAnsattApiDao = DelegatingEgenAnsattApiDao(personRepository)
    private val notatDao = DelegatingNotatDao(oppgaveRepository, notatRepository)
    private val oppgaveDao = DelegatingOppgaveDao(oppgaveRepository, behandlingRepository, vedtaksperiodeRepository)
    private val oppgaveApiDao = DelegatingOppgaveApiDao()
    private val personDao = DelegatingPersonDao(personRepository)
    private val påVentDao = DelegatingPåVentDao(påVentRepository, oppgaveRepository)
    private val saksbehandlerDao = DelegatingSaksbehandlerDao(saksbehandlerRepository)
    private val sykefraværstilfelleDao = DelegatingSykefraværstilfelleDao(overstyringRepository)

    private val legacyVedtaksperiodeRepository =
        DelegatingLegacyVedtaksperiodeRepository(
            vedtaksperiodeRepository,
            behandlingRepository,
            vedtakBegrunnelseRepository,
            varselRepository
        )
    private val legacyPersonRepository = DelegatingLegacyPersonRepository(
        personRepository,
        vedtaksperiodeRepository,
        legacyVedtaksperiodeRepository,
        behandlingRepository,
        varselRepository,
        vedtakBegrunnelseRepository,
        avviksvurderingRepository,
        sykefraværstilfelleDao,
    )

    private val arbeidsforholdDao = UnimplementedArbeidsforholdDao()
    private val arbeidsgiverApiDao = UnimplementedArbeidsgiverApiDao()
    private val automatiseringDao = UnimplementedAutomatiseringDao()
    private val behandlingApiRepository = UnimplementedBehandlingApiRepository()
    private val behandlingsstatistikkDao = UnimplementedBehandlingsstatistikkDao()
    private val definisjonDao = UnimplementedDefinisjonDao()
    private val legacyBehandlingDao = UnimplementedLegacyBehandlingDao()
    private val legacyVarselDao = UnimplementedLegacyVarselDao()
    private val metrikkDao = UnimplementedMetrikkDao()
    private val notatApiDao = UnimplementedNotatApiDao()
    private val overstyringApiDao = UnimplementedOverstyringApiDao()
    private val periodehistorikkDao = UnimplementedPeriodehistorikkDao()
    private val periodehistorikkApiDao = UnimplementedPeriodehistorikkApiDao()
    private val personApiDao = UnimplementedPersonApiDao()
    private val personinfoDao = UnimplementedPersoninfoDao()
    private val påVentApiDao = UnimplementedPåVentApiDao()
    private val risikovurderingDao = UnimplementedRisikovurderingDao()
    private val risikovurderingApiDao = UnimplementedRisikovurderingApiDao()
    private val stansAutomatiskBehandlingSaksbehandlerDao = UnimplementedStansAutomatiskBehandlingSaksbehandlerDao()
    private val tildelingDao = UnimplementedTildelingDao()
    private val tildelingApiDao = UnimplementedTildelingApiDao()
    private val utbetalingDao = UnimplementedUtbetalingDao()
    private val varselApiRepository = UnimplementedVarselApiRepository()
    private val vedtakBegrunnelseDao = UnimplementedVedtakBegrunnelseDao()
    private val vedtakDao = UnimplementedVedtakDao()
    private val vergemålDao = UnimplementedVergemålDao()
    private val vergemålApiDao = UnimplementedVergemålApiDao()
    private val åpneGosysOppgaverDao = UnimplementedÅpneGosysOppgaverDao()

    val daos = InMemoryDaos(
        abonnementApiDao = abonnementApiDao,
        annulleringRepository = annulleringRepository,
        arbeidsgiverApiDao = arbeidsgiverApiDao,
        behandlingApiRepository = behandlingApiRepository,
        behandlingsstatistikkDao = behandlingsstatistikkDao,
        commandContextDao = commandContextDao,
        definisjonDao = definisjonDao,
        dialogDao = dialogDao,
        dokumentDao = dokumentDao,
        egenAnsattDao = egenAnsattDao,
        egenAnsattApiDao = egenAnsattApiDao,
        legacyBehandlingDao = legacyBehandlingDao,
        legacyVarselDao = legacyVarselDao,
        meldingDao = meldingDao,
        meldingDuplikatkontrollDao = meldingDuplikatkontrollDao,
        notatDao = notatDao,
        notatApiDao = notatApiDao,
        oppgaveDao = oppgaveDao,
        oppgaveApiDao = oppgaveApiDao,
        oppgaveRepository = oppgaveRepository,
        opptegnelseDao = opptegnelseDao,
        overstyringApiDao = overstyringApiDao,
        periodehistorikkDao = periodehistorikkDao,
        periodehistorikkApiDao = periodehistorikkApiDao,
        personDao = personDao,
        personApiDao = personApiDao,
        personinfoDao = personinfoDao,
        poisonPillDao = poisonPillDao,
        påVentDao = påVentDao,
        påVentApiDao = påVentApiDao,
        reservasjonDao = reservasjonDao,
        risikovurderingApiDao = risikovurderingApiDao,
        saksbehandlerDao = saksbehandlerDao,
        saksbehandlerRepository = saksbehandlerRepository,
        stansAutomatiskBehandlingDao = stansAutomatiskBehandlingDao,
        stansAutomatiskBehandlingSaksbehandlerDao = stansAutomatiskBehandlingSaksbehandlerDao,
        tildelingDao = tildelingDao,
        tildelingApiDao = tildelingApiDao,
        varselApiRepository = varselApiRepository,
        vedtakBegrunnelseDao = vedtakBegrunnelseDao,
        vedtakDao = vedtakDao,
        vergemålApiDao = vergemålApiDao,
    )

    val sessionFactory = InMemorySessionFactory(
        sessionContext = InMemorySessionContext(
            annulleringRepository = annulleringRepository,
            arbeidsforholdDao = arbeidsforholdDao,
            arbeidsgiverRepository = arbeidsgiverRepository,
            automatiseringDao = automatiseringDao,
            avviksvurderingRepository = avviksvurderingRepository,
            behandlingRepository = behandlingRepository,
            commandContextDao = commandContextDao,
            dialogDao = dialogDao,
            dialogRepository = dialogRepository,
            dokumentDao = dokumentDao,
            egenAnsattDao = egenAnsattDao,
            legacyBehandlingDao = legacyBehandlingDao,
            legacyPersonRepository = legacyPersonRepository,
            legacyVedtaksperiodeRepository = legacyVedtaksperiodeRepository,
            meldingDao = meldingDao,
            metrikkDao = metrikkDao,
            notatDao = notatDao,
            notatRepository = notatRepository,
            oppgaveDao = oppgaveDao,
            oppgaveRepository = oppgaveRepository,
            opptegnelseDao = opptegnelseDao,
            overstyringRepository = overstyringRepository,
            periodehistorikkDao = periodehistorikkDao,
            personDao = personDao,
            personRepository = personRepository,
            personPseudoIdDao = personPseudoIdDao,
            påVentDao = påVentDao,
            påVentRepository = påVentRepository,
            reservasjonDao = reservasjonDao,
            risikovurderingDao = risikovurderingDao,
            saksbehandlerDao = saksbehandlerDao,
            saksbehandlerRepository = saksbehandlerRepository,
            stansAutomatiskBehandlingDao = stansAutomatiskBehandlingDao,
            stansAutomatiskBehandlingSaksbehandlerDao = stansAutomatiskBehandlingSaksbehandlerDao,
            sykefraværstilfelleDao = sykefraværstilfelleDao,
            tildelingDao = tildelingDao,
            tilkommenInntektRepository = tilkommenInntektRepository,
            totrinnsvurderingRepository = totrinnsvurderingRepository,
            utbetalingDao = utbetalingDao,
            varselRepository = varselRepository,
            varseldefinisjonRepository = varseldefinisjonRepository,
            vedtaksperiodeRepository = vedtaksperiodeRepository,
            vedtakBegrunnelseRepository = vedtakBegrunnelseRepository,
            vedtakDao = vedtakDao,
            vergemålDao = vergemålDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        )
    )
}
