package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.PgAbonnementDao
import javax.sql.DataSource

class RepositoriesImpl(dataSource: DataSource) : Repositories {
    override val abonnementDao = PgAbonnementDao(dataSource)
    override val annulleringRepository = PgAnnulleringRepository(dataSource)

    override fun withSessionContext(session: Session) = SessionContextImpl(session)
}
