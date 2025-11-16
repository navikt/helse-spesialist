package no.nav.helse.spesialist.application

import no.nav.helse.db.SessionContext

class InMemorySessionContext(
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val behandlingRepository: InMemoryBehandlingRepository,
    override val dialogRepository: InMemoryDialogRepository,
    override val egenAnsattDao: DelegatingEgenAnsattDao,
    override val notatRepository: InMemoryNotatRepository,
    override val opptegnelseDao: InMemoryOpptegnelseDao,
    override val overstyringRepository: InMemoryOverstyringRepository,
    override val personRepository: InMemoryPersonRepository,
    override val påVentRepository: InMemoryPåVentRepository,
    override val saksbehandlerRepository: InMemorySaksbehandlerRepository,
    override val varseldefinisjonRepository: InMemoryVarseldefinisjonRepository,
    override val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
    override val dokumentDao: InMemoryDokumentDao,
    override val oppgaveRepository: InMemoryOppgaveRepository,
    override val stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
) : SessionContext {
    override val varselRepository = InMemoryVarselRepository()
    override val vedtakBegrunnelseRepository = InMemoryVedtakBegrunnelseRepository()

    override val arbeidsforholdDao = UnimplementedArbeidsforholdDao()
    override val arbeidsgiverRepository = InMemoryArbeidsgiverRepository()
    override val automatiseringDao = UnimplementedAutomatiseringDao()
    override val avviksvurderingRepository = InMemoryAvviksvurderingRepository()
    override val commandContextDao = InMemoryCommandContextDao()
    override val dialogDao = DelegatingDialogDao(dialogRepository)
    override val legacyBehandlingDao = UnimplementedLegacyBehandlingDao()
    override val sykefraværstilfelleDao = DelegatingSykefraværstilfelleDao(overstyringRepository)
    override val legacyVedtaksperiodeRepository =
        DelegatingLegacyVedtaksperiodeRepository(
            vedtaksperiodeRepository,
            behandlingRepository,
            vedtakBegrunnelseRepository,
            varselRepository
        )
    override val legacyPersonRepository = DelegatingLegacyPersonRepository(
        personRepository,
        vedtaksperiodeRepository,
        legacyVedtaksperiodeRepository,
        behandlingRepository,
        varselRepository,
        vedtakBegrunnelseRepository,
        avviksvurderingRepository,
        sykefraværstilfelleDao,
    )
    override val meldingDao = InMemoryMeldingDao()
    override val metrikkDao = UnimplementedMetrikkDao()
    override val notatDao = DelegatingNotatDao(oppgaveRepository, notatRepository)
    override val oppgaveDao = DelegatingOppgaveDao(oppgaveRepository, behandlingRepository, vedtaksperiodeRepository)
    override val periodehistorikkDao = UnimplementedPeriodehistorikkDao()
    override val personDao = DelegatingPersonDao(personRepository)
    override val personPseudoIdDao = InMemoryPersonPseudoIdDao()
    override val påVentDao = DelegatingPåVentDao(påVentRepository, oppgaveRepository)
    override val reservasjonDao = InMemoryReservasjonDao(saksbehandlerRepository)
    override val risikovurderingDao = UnimplementedRisikovurderingDao()
    override val saksbehandlerDao = DelegatingSaksbehandlerDao(saksbehandlerRepository)
    override val stansAutomatiskBehandlingSaksbehandlerDao = UnimplementedStansAutomatiskBehandlingSaksbehandlerDao()
    override val tildelingDao = UnimplementedTildelingDao()
    override val tilkommenInntektRepository = InMemoryTilkommenInntektRepository()
    override val totrinnsvurderingRepository = InMemoryTotrinnsvurderingRepository()
    override val utbetalingDao = UnimplementedUtbetalingDao()
    override val vedtakDao = UnimplementedVedtakDao()
    override val vergemålDao = UnimplementedVergemålDao()
    override val åpneGosysOppgaverDao = UnimplementedÅpneGosysOppgaverDao()
}
