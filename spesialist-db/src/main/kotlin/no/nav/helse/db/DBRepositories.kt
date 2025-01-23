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

    override fun withSessionContext(session: Session) = DBSessionContext(session)
}
