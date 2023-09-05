package no.nav.helse

import java.util.UUID

val testEnv = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }
fun idForGruppe(gruppe: Gruppe) = Tilgangsgrupper(testEnv).gruppeId(gruppe).toString()

