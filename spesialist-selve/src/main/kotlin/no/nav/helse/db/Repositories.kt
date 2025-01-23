package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.AbonnementDao

interface Repositories {
    val abonnementDao: AbonnementDao
    val annulleringRepository: AnnulleringRepository
    val behandlingsstatistikkDao: BehandlingsstatistikkDao
    val opptegnelseRepository: OpptegnelseRepository

    fun withSessionContext(session: Session): SessionContext
}
