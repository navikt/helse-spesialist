package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.operations.Mutation
import no.nav.helse.mediator.api.graphql.schema.Kommentar
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

    fun leggTilKommentar(notatId: Int, tekst: String): Kommentar? {
        val kommentar = notatDao.leggTilKommentar(notatId, tekst) ?: return null

        return Kommentar(
            id = kommentar.id,
            tekst = kommentar.tekst,
            feilregistrert_tidspunkt = null,
        )
    }
}