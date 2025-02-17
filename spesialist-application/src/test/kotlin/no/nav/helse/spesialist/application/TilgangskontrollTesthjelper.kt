package no.nav.helse.spesialist.application

import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import java.util.UUID

val testEnv = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }

fun idForGruppe(gruppe: Gruppe) = SpeilTilgangsgrupper(testEnv).gruppeId(gruppe).toString()

object TilgangskontrollForTestHarIkkeTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean {
        return false
    }
}
