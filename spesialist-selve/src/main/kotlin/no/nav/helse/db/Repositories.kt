package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.AbonnementDao

interface Repositories {
    val abonnementDao: AbonnementDao
    val annulleringRepository: AnnulleringRepository
    val behandlingsstatistikkDao: BehandlingsstatistikkDao
    val opptegnelseRepository: OpptegnelseRepository
    val avviksvurderingDao: AvviksvurderingDao
    val dialogDao: DialogDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val periodehistorikkDao: PeriodehistorikkDao
    val totrinnsvurderingDao: TotrinnsvurderingDao
    val vedtakDao: VedtakDao
    val poisonPillDao: PoisonPillDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val tildelingDao: TildelingDao
    val vedtakBegrunnelseDao: VedtakBegrunnelseDao
    val generasjonDao: GenerasjonDao
    val reservasjonDao: ReservasjonDao
    val commandContextDao: CommandContextDao
    val dokumentDao: DokumentDao
    val egenAnsattDao: EgenAnsattDao
    val overstyringDao: OverstyringDao
    val personDao: PersonDao
    val påVentDao: PåVentDao
    val definisjonDao: DefinisjonDao
    val varselDao: VarselDao

    fun withSessionContext(session: Session): SessionContext
}
