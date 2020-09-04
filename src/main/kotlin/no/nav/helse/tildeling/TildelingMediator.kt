package no.nav.helse.tildeling

import kotliquery.sessionOf
import java.util.*
import javax.sql.DataSource

class TildelingMediator(private val dataSource: DataSource) {
    fun hentSaksbehandlerFor(oppgavereferanse: UUID): UUID? = sessionOf(dataSource).use { session ->
        session.hentSaksbehandlerFor(oppgavereferanse)
    }

    fun tildelOppgaveTilSaksbehandler(oppgavereferanse: UUID, saksbehandlerreferanse: UUID) {
        sessionOf(dataSource).use { session ->
            session.tildelOppgave(oppgavereferanse, saksbehandlerreferanse)
        }
    }

    fun fjernTildeling(oppgavereferanse: UUID) {
        sessionOf(dataSource).use { session ->
            session.slettOppgavetildeling(oppgavereferanse)
        }
    }
}
