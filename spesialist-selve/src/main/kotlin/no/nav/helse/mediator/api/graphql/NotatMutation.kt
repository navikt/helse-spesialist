package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.operations.Mutation
import no.nav.helse.spesialist.api.notat.NotatDao

class NotatMutation(private val notatDao: NotatDao) : Mutation {
    fun feilregistrerNotat(id: Int): Boolean {
        val antallOppdatert = notatDao.feilregistrerNotat(id)
        return antallOppdatert > 0
    }

    fun feilregistrerKommentar(id: Int): Boolean {
        val antallOppdatert = notatDao.feilregistrerKommentar(id)
        return antallOppdatert > 0
    }
}