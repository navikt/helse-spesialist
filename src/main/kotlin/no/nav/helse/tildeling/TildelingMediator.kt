package no.nav.helse.tildeling

import kotliquery.sessionOf
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import java.util.*
import javax.sql.DataSource

class TildelingMediator(private val dataSource: DataSource) {
    fun hentSaksbehandlerFor(oppgavereferanse: UUID): String? = sessionOf(dataSource).use { session ->
        session.hentSaksbehandlerEpostFor(oppgavereferanse)
    }

    fun tildelOppgaveTilSaksbehandler(
        oppgavereferanse: UUID,
        saksbehandlerreferanse: UUID,
        epostadresse: String,
        navn: String
    ) {
        sessionOf(dataSource).use { session ->
            val saksbehandlerFor = session.hentSaksbehandlerNavnFor(oppgavereferanse)
            if (saksbehandlerFor != null) {
                throw ModellFeil(OppgaveErAlleredeTildelt(saksbehandlerFor))
            }
            session.persisterSaksbehandler(saksbehandlerreferanse, navn, epostadresse)
            session.tildelOppgave(oppgavereferanse, saksbehandlerreferanse)
        }
    }

    fun fjernTildeling(oppgavereferanse: UUID) {
        sessionOf(dataSource).use { session ->
            session.slettOppgavetildeling(oppgavereferanse)
        }
    }
}
