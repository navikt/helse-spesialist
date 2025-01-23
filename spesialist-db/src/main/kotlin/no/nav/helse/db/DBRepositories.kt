package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.PgAbonnementDao
import javax.sql.DataSource

class DBRepositories(dataSource: DataSource) : Repositories {
    override val abonnementDao = PgAbonnementDao(dataSource)
    override val annulleringRepository = PgAnnulleringRepository(dataSource)
    override val behandlingsstatistikkDao = PgBehandlingsstatistikkDao(dataSource)
    override val opptegnelseRepository = PgOpptegnelseRepository(dataSource)
    override val avviksvurderingDao = PgAvviksvurderingDao(dataSource)
    override val dialogDao = PgDialogDao(dataSource)
    override val notatDao = PgNotatDao(dataSource)
    override val oppgaveDao = PgOppgaveDao(dataSource)
    override val periodehistorikkDao = PgPeriodehistorikkDao(dataSource)
    override val totrinnsvurderingDao = PgTotrinnsvurderingDao(dataSource)
    override val vedtakDao = PgVedtakDao(dataSource)
    override val poisonPillDao = PgPoisonPillDao(dataSource)
    override val saksbehandlerDao = PgSaksbehandlerDao(dataSource)
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(dataSource)
    override val tildelingDao = PgTildelingDao(dataSource)
    override val vedtakBegrunnelseDao = PgVedtakBegrunnelseDao(dataSource)
    override val generasjonDao = PgGenerasjonDao(dataSource)
    override val reservasjonDao = PgReservasjonDao(dataSource)
    override val commandContextDao = PgCommandContextDao(dataSource)
    override val dokumentDao = PgDokumentDao(dataSource)
    override val egenAnsattDao = PgEgenAnsattDao(dataSource)
    override val overstyringDao = PgOverstyringDao(dataSource)

    override fun withSessionContext(session: Session) = DBSessionContext(session)
}
