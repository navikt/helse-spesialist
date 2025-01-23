package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.AbonnementDao

interface Repositories {
    val abonnementDao: AbonnementDao
    val annulleringRepository: AnnulleringRepository
    val behandlingsstatistikkDao: BehandlingsstatistikkDao

    fun withSessionContext(session: Session): SessionContext
}
