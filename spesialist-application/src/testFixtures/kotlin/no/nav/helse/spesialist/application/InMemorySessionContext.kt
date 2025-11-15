package no.nav.helse.spesialist.application

import no.nav.helse.db.SessionContext

class InMemorySessionContext(
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val behandlingRepository: InMemoryBehandlingRepository,
    override val dialogRepository: InMemoryDialogRepository,
    override val egenAnsattDao: InMemoryEgenAnsattDao,
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

    override val arbeidsforholdDao = InMemoryArbeidsforholdDao()
    override val arbeidsgiverRepository = InMemoryArbeidsgiverRepository()
    override val automatiseringDao = InMemoryAutomatiseringDao()
    override val avviksvurderingRepository = InMemoryAvviksvurderingRepository()
    override val commandContextDao = InMemoryCommandContextDao()
    override val dialogDao = InMemoryDialogDao(dialogRepository)
    override val legacyBehandlingDao = InMemoryLegacyBehandlingDao()
    override val sykefraværstilfelleDao = InMemorySykefraværstilfelleDao(overstyringRepository)
    override val legacyVedtaksperiodeRepository =
        InMemoryLegacyVedtaksperiodeRepository(
            vedtaksperiodeRepository,
            behandlingRepository,
            vedtakBegrunnelseRepository,
            varselRepository
        )
    override val legacyPersonRepository = InMemoryLegacyPersonRepository(
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
    override val metrikkDao = InMemoryMetrikkDao()
    override val notatDao = InMemoryNotatDao(oppgaveRepository, notatRepository)
    override val oppgaveDao = InMemoryOppgaveDao(oppgaveRepository, behandlingRepository, vedtaksperiodeRepository)
    override val periodehistorikkDao = InMemoryPeriodehistorikkDao()
    override val personDao = InMemoryPersonDao(personRepository)
    override val personPseudoIdDao = InMemoryPersonPseudoIdDao()
    override val påVentDao = InMemoryPåVentDao(påVentRepository, oppgaveRepository)
    override val reservasjonDao = InMemoryReservasjonDao(saksbehandlerRepository)
    override val risikovurderingDao = InMemoryRisikovurderingDao()
    override val saksbehandlerDao = InMemorySaksbehandlerDao(saksbehandlerRepository)
    override val stansAutomatiskBehandlingSaksbehandlerDao = InMemoryStansAutomatiskBehandlingSaksbehandlerDao()
    override val tildelingDao = InMemoryTildelingDao()
    override val tilkommenInntektRepository = InMemoryTilkommenInntektRepository()
    override val totrinnsvurderingRepository = InMemoryTotrinnsvurderingRepository()
    override val utbetalingDao = InMemoryUtbetalingDao()
    override val vedtakDao = InMemoryVedtakDao()
    override val vergemålDao = InMemoryVergemålDao()
    override val åpneGosysOppgaverDao = InMemoryÅpneGosysOppgaverDao()
}
