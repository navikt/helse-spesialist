package no.nav.helse

import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import java.util.UUID

val testEnv = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }

fun idForGruppe(gruppe: Gruppe) = SpeilTilgangsgrupper(testEnv).gruppeId(gruppe).toString()
