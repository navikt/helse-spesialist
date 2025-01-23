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

    fun withSessionContext(session: Session): SessionContext
}
