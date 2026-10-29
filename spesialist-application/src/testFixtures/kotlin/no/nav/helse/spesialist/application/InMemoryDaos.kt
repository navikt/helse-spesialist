package no.nav.helse.spesialist.application

import no.nav.helse.db.Daos

class InMemoryDaos(
    override val behandlingsstatistikkDao: InMemoryBehandlingsstatistikkDao,
    override val commandContextDao: InMemoryCommandContextDao,
    override val dokumentDao: InMemoryDokumentDao,
    override val meldingDao: InMemoryMeldingDao,
    override val meldingDuplikatkontrollDao: NoopMeldingDuplikatkontrollDao,
    override val oppgaveDao: DelegatingOppgaveDao,
    override val periodehistorikkApiDao: UnimplementedPeriodehistorikkApiDao,
    override val personDao: DelegatingPersonDao,
    override val poisonPillDao: NoopPoisonPillDao,
    override val varselApiRepository: UnimplementedVarselApiRepository,
    override val vedtakBegrunnelseDao: InMemoryVedtakBegrunnelseDao,
) : Daos
