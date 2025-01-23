package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.AbonnementDao

interface RepositoryFactory {
    fun createAbonnementDao(): AbonnementDao

    fun createAnnulleringRepository(): AnnulleringRepository

    fun sessionContextFrom(session: Session): SessionContext
}
