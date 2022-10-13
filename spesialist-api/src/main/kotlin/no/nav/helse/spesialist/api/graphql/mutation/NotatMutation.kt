package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import java.time.format.DateTimeFormatter
import no.nav.helse.spesialist.api.graphql.schema.Kommentar
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

    fun leggTilKommentar(notatId: Int, tekst: String, saksbehandlerident: String): Kommentar? {
        val kommentar = notatDao.leggTilKommentar(notatId, tekst, saksbehandlerident) ?: return null

        return Kommentar(
            id = kommentar.id,
            tekst = kommentar.tekst,
            opprettet = kommentar.opprettet.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            saksbehandlerident = kommentar.saksbehandlerident,
            feilregistrert_tidspunkt = null,
        )
    }
}