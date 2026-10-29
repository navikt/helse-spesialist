package no.nav.helse.spesialist.application

import no.nav.helse.db.Daos

class InMemoryDaos(
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val behandlingsstatistikkDao: InMemoryBehandlingsstatistikkDao,
    override val commandContextDao: InMemoryCommandContextDao,
    override val dialogDao: DelegatingDialogDao,
    override val dokumentDao: InMemoryDokumentDao,
    override val legacyBehandlingDao: UnimplementedLegacyBehandlingDao,
    override val meldingDao: InMemoryMeldingDao,
    override val meldingDuplikatkontrollDao: NoopMeldingDuplikatkontrollDao,
    override val notatDao: DelegatingNotatDao,
    override val oppgaveDao: DelegatingOppgaveDao,
    override val oppgaveApiDao: DelegatingOppgaveApiDao,
    override val oppgaveRepository: InMemoryOppgaveRepository,
    override val overstyringApiDao: UnimplementedOverstyringApiDao,
    override val periodehistorikkDao: InMemoryPeriodehistorikkDao,
    override val periodehistorikkApiDao: UnimplementedPeriodehistorikkApiDao,
    override val personDao: DelegatingPersonDao,
    override val poisonPillDao: NoopPoisonPillDao,
    override val påVentDao: DelegatingPåVentDao,
    override val reservasjonDao: InMemoryReservasjonDao,
    override val saksbehandlerDao: DelegatingSaksbehandlerDao,
    override val saksbehandlerRepository: InMemorySaksbehandlerRepository,
    override val tildelingDao: DelegatingTildelingDao,
    override val varselApiRepository: UnimplementedVarselApiRepository,
    override val vedtakBegrunnelseDao: InMemoryVedtakBegrunnelseDao,
    override val vedtakDao: DelegatingVedtakDao,
) : Daos
