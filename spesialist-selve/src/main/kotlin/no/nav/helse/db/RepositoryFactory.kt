package no.nav.helse.db

import no.nav.helse.db.api.AbonnementDao

interface RepositoryFactory {
    fun createAbonnementDao(): AbonnementDao
}
